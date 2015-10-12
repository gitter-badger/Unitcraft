package unitcraft.server

import org.json.simple.JSONObject
import org.mindrot.BCrypt
import unitcraft.inject.inject
import java.util.*
import java.util.concurrent.Executors

class Server {
    val wser: Wser by inject()
    val log: Log by inject()
    val threadMain = Executors.newSingleThreadExecutor()
    val threadAux = Executors.newSingleThreadExecutor()
    val users: Users by inject()
    val bttler: Bttler by inject()
    val ssns = HashMap<String, Ssn>()
    val bttls = HashMap<Id, Bttl>()

    lateinit var ssn: Ssn
    lateinit var bttl: Bttl

    init {
        wser.onOpen { key, isLocal -> onOpen(key, isLocal) }
        wser.onMsg { key, msg -> onMsg(key, msg) }
        wser.onClose { key -> onClose(key) }
    }

    fun start() {
        wser.start()
        println("Server started")
    }

    fun onMsg(key: String, msg: String) {
        threadMain.execute {
            try {
                ssn = ssns[key]!!
                if (msg.isEmpty()) throw Violation("msg is empty")
                if (msg.length() > 20) {
                    throw Violation("msg len > 20: ${msg.substring(0, 20)}...")
                }
                log.msg(msg)
                val prm = Prm(msg[1, msg.length()].toString())
                when (msg[0]) {
                    'q' -> send("q")
                    'n' -> reg(prm)
                    'l' -> onLogin(prm)
                    'k' -> changeNick(prm)
                    'p' -> onPlay(prm)
                    't' -> onVsRobot(prm)
                    'm' -> vsRobotMission(prm)
                    'a' -> akt(prm)
                    'r' -> refresh(prm)
                    'w' -> land(prm)
                    'y' -> accept(prm)
                    'c' -> invite(prm)
                    'd' -> onDecline(prm)
                    else -> throw Violation("unknown msg: " + msg)
                }
            } catch(ex: Violation) {
                log.violation(ex)
                wser.close(key)
            } catch (ex: Throwable) {
                log.error(ex)
                wser.close(key)
            }
        }
    }

    fun onOpen(key: String, isLocal: Boolean) {
        threadMain.execute {
            ssn = Ssn(key, isLocal)
            ssns[key] = ssn
            log.open()
            if (isLocal) {
                var id = Id("dev" + ssns.size())
                val user = users.get(id)
                if (user == null) users.add(id, "")
                loginOk(id, 1)
            }
        }
    }

    fun onClose(key: String) {
        threadMain.execute {
            log.close()
            ssn = ssns[key]!!
            if (ssn.isLogin) decline()
            ssns.remove(key)
        }
    }

    fun reg(prm: Prm) {
        ensureUnlogin("reg")
        prm.ensureEmpty()
        val pw = users.newKey()
        threadAux.execute {
            val digest = BCrypt.hashpw(pw, BCrypt.gensalt())!!
            threadMain.execute {
                ssn.regData = RegData(pw, digest)
                send("n" + pw)
            }
        }
    }

    fun onLogin(prm: Prm) {
        ensureUnlogin("login")
        prm.ensureSize(2)
        val pw = prm.str(0, 15, 15)
        val mission = prm.mission(1)
        val regData = ssn.regData
        if (regData != null && pw == regData.pw) {
            val id = Users.keyToId(regData.pw)
            users.add(id, regData.digest)
            ssn.regData = null
            loginOk(id, mission)
        } else {
            var id = Users.keyToId(pw)
            val user = users[id]
            if (user != null) {
                threadAux.execute {
                    val ok = if (BCrypt.checkpw(pw, user.digest)) user.id else null
                    threadMain.execute {
                        loginOk(ok, mission)
                    }
                }
            } else {
                send("w")
            }
        }
    }

    fun loginOk(id: Id?, mission: Int) {
        if (id != null) {
            ssn.login(id)
            sendUser()
            ssn.bttl = bttls[id]
            if (ssn.bttl != null)
                send(bttler.refresh(ssn.id))
            else
                vsRobot(mission)
        } else {
            send("w")
        }
    }

    fun changeNick(prm: Prm) {
        ensureLogin("changeNick")
        prm.ensureSize(1)
        val nick = prm.str(0, 3, 15)
        users.changeNick(ssn.id, nick)
        sendUser()
    }

    fun onPlay(prm: Prm) {
        ensureLogin("play")
        prm.ensureSize(2)
        ssn.ensureState(Status.online)
        val min = prm.bet(0)
        val max = prm.bet(1)
        if (min > max) throw Violation("min bet($min) > max bet($max)")
        //TODO check balance
        play(min, max)
    }

    private fun play(min:Int,max:Int) {
        val play = Play(min,max)
        val ssnsInQue = ssns.values().filter { it.play != null && it.play?.match==null }
        if (ssnsInQue.size() >= 1) {
            val ssnFinded = ssnsInQue.first()
            ssn.play!!.match = Match(ssnFinded, 1)
            ssnFinded.play!!.match = Match(ssn, 1)
            sendStatus()
            sendStatus(ssnFinded)
        } else {
            ssn.play = play
            ssn.invite = null
            sendStatus()
        }
    }

    fun onVsRobot(prm: Prm) {
        ensureLogin("vsRobot")
        prm.ensureEmpty()
        vsRobot()
    }

    fun vsRobotMission(prm: Prm) {
        ensureLogin("vsRobotMission")
        prm.ensureSize(1)
        vsRobot(prm.mission(0))
    }

    fun akt(prm: Prm) {
        ensureLogin("akt")
        bttl = bttls[ssn.id]!!
        send(bttler.cmd(ssn.id, prm))
        if (bttl.idWin() != null) {
            log.end(bttl.idWin().toString())
            sendStatus(ssns[bttl.idPrim]!!)
            sendStatus(ssns[bttl.idSec]!!)
        }
    }

    fun land(prm: Prm) {
        ensureLogin("land")
        prm.ensureEmpty()
        println(bttler.land(ssn.id))
    }

    fun accept(prm: Prm) {
        ensureLogin("matchAccept")
        prm.ensureEmpty()
        ssn.ensureState(Status.match)
        val ssnVs = ssn.play!!.match!!.ssnVs
        if (ssnVs.play!!.match!!.accepted) {
            vsPlayer(ssn, ssnVs, ssn.play!!.match!!.bet)
        } else {
            ssn.play!!.match!!.accepted = true
            sendStatus()
        }
    }

    fun invite(prm: Prm) {
        ensureLogin("invite")
        prm.ensureSize(2)
        //rooms.invite(ssn.id, Id(prm.str(0, 4, 4)), prm.bet(1))
        ssn.ensureState(Status.online)
        val bet = prm.bet(1)
        val idVs = Id(prm.str(0, 4, 4))
        // TODO check balance
        val ssnInvite = ssns[idVs]
        val invite = ssnInvite?.invite
        if (ssnInvite != null && invite != null && ssn.id == invite.idVs && bet == invite.bet) {
            vsPlayer(ssn, ssnInvite, bet)
        } else {
            ssn.invite = Invite(idVs, bet)
            ssn.play = null
            sendStatus()
        }
    }

    fun onDecline(prm: Prm) {
        ensureLogin("decline")
        prm.ensureEmpty()
        decline()
    }

    private fun decline() {
        ssn.play?.match?.ssnVs?.play?.match=null
        ssn.play = null
        ssn.invite = null
    }

    fun refresh(prm: Prm) {
        ensureLogin("refresh")
        prm.ensureEmpty()
        bttl = ssn.bttl!!
        send(bttler.refresh(ssn.id))
    }

    fun ensureLogin(cmd: String) {
        if (!ssn.isLogin) throw Violation("need login for $cmd")
    }

    fun ensureAdmin(cmd: String) {
        if (!ssn.isLogin || ssn.id.toString() != "0000") throw Violation("need admin for $cmd")
    }

    fun ensureUnlogin(cmd: String) {
        if (ssn.isLogin) throw Violation("need unlogin for ${cmd}")
    }

    fun sendUser() {
        val user = users[ssn.id]!!
        val obj = JSONObject()
        obj["id"] = user.id.toString()
        obj["nick"] = user.nick
        obj["balance"] = user.balance
        send("u" + obj)
    }

    private fun vsRobot(mission: Int? = null) {
        ssn.ensureStateNotGame()
        log.vsRobot(ssn.id)
        //val room = Room(log, send, id, null, 0, creatorGame.createGame(mission))
        var bttl = Bttl(ssn.id)
        bttls[ssn.id] = bttl
        ssn.bttl = bttl
        send(bttler.start(mission,true))
    }

    private fun vsPlayer(ssnA: Ssn, ssnB: Ssn, bet: Int) {
        ssnA.ensureStateNotGame()
        ssnB.ensureStateNotGame()
        log.vsPlayer(ssnA.id, ssnB.id)
        bttl = Bttl(ssnA.id, ssnB.id, bet)
        bttls[ssnA.id] = bttl
        bttls[ssnB.id] = bttl
        ssnA.bttl = bttl
        ssnB.bttl = bttl
        ssnA.invite = null
        ssnB.invite = null
        ssnA.play = null
        ssnB.play = null
        sendStatus(ssnA)
        sendStatus(ssnB)
        send(bttler.start(null,false))
    }

    fun send(msg: String) {
        wser.send(ssn.key, msg)
    }

    fun send(chain: Chain) {
        chain.list.forEach { wser.send(ssns[it.first]!!.key, it.second) }
    }

    fun sendStatus(s: Ssn = ssn) {
        send("s" + s.status())
    }
}

class Ssn(val key: String, val isLocal: Boolean) {

    private var _id: Id? = null

    val id: Id
        get() = _id!!

    val isLogin: Boolean
        get() = _id != null

    fun login(id: Id) {
        if (isLogin) throw Err("already isLogin")
        _id = id
    }

    var regData: RegData? = null
    var play: Play? = null
    var invite: Invite? = null
    var bttl: Bttl? = null

    fun status(): Status {
        return when {
            bttl?.let { it.sideRobot==null && it.idWin() == null } ?: false -> Status.game
            play?.match?.let { it.accepted } ?: false -> Status.wait
            play?.match != null -> Status.match
            play != null -> Status.queue
            invite != null -> Status.invite
            else -> Status.online
        }
    }

    fun ensureStateNotGame() {
        if (status() == Status.game) throw Violation("State.game is not allowed")
    }

    fun ensureState(state: Status) {
        val actual = status()
        if (actual != state) throw Violation("wrong state $actual expected $state")
    }
}

data class RegData(val pw: String, val digest: String)

class Play(val min: Int, val max: Int) {
    var match: Match? = null

    init {
        if (min < 1 || min > max) throw IllegalArgumentException("BetRange min $min max $max")
    }
}

class Match(val ssnVs: Ssn, val bet: Int){
    var accepted: Boolean = false
}

data class Invite(val idVs: Id, val bet: Int) {init {
    if (bet < 0) throw IllegalArgumentException("Invite bet $bet")
}
}

//состояние пользователя относительно очереди
//отображается в 3м квадрате в интерфейсе
enum class Status {
    // свободен
    online,
    // выслал приглашение
    invite,
    // в очереди
    queue,
    // матч найден
    match,
    // матч найден и принят пользователем
    wait,
    // играет с человеком
    game
}
