package unitcraft.server

import unitcraft
import unitcraft.game.Unitcraft
import java.util.concurrent.Executors
import java.util.HashMap
import org.json.simple.JSONObject
import org.mindrot.BCrypt


class Server(val log: Log) : Sender{
    val threadMain = Executors.newSingleThreadExecutor()
    val threadAux = Executors.newSingleThreadExecutor()
    val users = Users(log)
    val rooms = Rooms(log, this, Unitcraft())
    val wss = HashMap<Id, Ws>()

    fun onMsg(ws: Ws, msg: String) {
        threadMain.execute {
            try {
                if (msg.isEmpty()) throw Violation("msg is empty")
                if (msg.length() > 20) {
                    throw Violation("msg len > 20: ${msg.substring(0, 20)}...")
                }
                log.msg(msg)
                val prm = Prm(msg[1, msg.length()].toString())
                when (msg[0]) {
                    'q' -> ws.send("q");
                    'n' -> reg(ws, prm)
                    'l' -> login(ws, prm)
                    'k' -> changeNick(ws, prm)
                    'p' -> play(ws, prm)
                    't' -> vsRobot(ws, prm)
                    'm' -> vsRobotMission(ws, prm)
                    'a' -> akt(ws, prm)
                    'r' -> refresh(ws, prm)
                    'w' -> land(ws, prm)
                    'y' -> accept(ws, prm)
                    'c' -> invite(ws, prm)
                    'd' -> decline(ws, prm)
                    else -> throw Violation("unknown msg: " + msg)
                }
            } catch(ex: Violation) {
                log.violation(ex)
                ws.close()
            } catch (ex: Throwable) {
                log.error(ex)
                ws.close()
            }
        }
    }

    fun onOpen(ws: Ws) {
        threadMain.execute {
            log.open()
            if(ws.ip()=="127.0.0.1") {
                var id = Id("dev"+users.users.size())
                val user = users.get(id)
                if (user == null) users.add(id, "")
                loginOk(ws, id, 1)
            }
        }
    }

    fun onClose(ws: Ws) {
        threadMain.execute {
            log.close()
            if (ws.isLogin) {
                rooms.close(ws.id)
            }
            if(ws.isLogin) wss.remove(ws.id)
        }
    }

    fun reg(ws: Ws, prm: Prm) {
        ensureUnlogin(ws, "reg")
        prm.ensureEmpty()
        val key = users.newKey()
        threadAux.execute {
            val digest = BCrypt.hashpw(key, BCrypt.gensalt())!!
            threadMain.execute {
                users.add(Users.keyToId(key), digest)
                ws.send("n" + key)
            }
        }
    }

    fun login(ws: Ws, prm: Prm) {
        ensureUnlogin(ws, "login")
        prm.ensureSize(2)
        val key = prm.str(0, 15, 15)
        var id = Users.keyToId(key)
        val mission = prm.mission(1)
        val user = users[id]
        if (user != null) {
            threadAux.execute {
                val ok = if (BCrypt.checkpw(key, user.digest)) user.id else null
                threadMain.execute {
                    loginOk(ws, ok, mission)
                }
            }
        } else {
            ws.send("w")
        }
    }

    fun loginOk(ws: Ws, id: Id?, mission: Int) {
        if (id != null) {
            ws.login(id)
            wss[ws.id] = ws
            sendUser(ws)
            rooms.login(ws.id,mission)
        } else {
            ws.send("w")
        }
    }

    fun changeNick(ws: Ws, prm: Prm) {
        ensureLogin(ws, "changeNick")
        prm.ensureSize(1)
        val nick = prm.str(0, 3, 15)
        users.changeNick(ws.id, nick)
        sendUser(ws)
    }

    fun play(ws: Ws, prm: Prm) {
        ensureLogin(ws, "play")
        prm.ensureSize(2)
        rooms.queue(ws.id, prm.bet(0), prm.bet(1))
    }

    fun vsRobot(ws: Ws, prm: Prm) {
        ensureLogin(ws, "vsRobot")
        prm.ensureEmpty()
        rooms.vsRobot(ws.id)
    }

    fun vsRobotMission(ws: Ws, prm: Prm) {
        ensureLogin(ws, "vsRobotMission")
        prm.ensureSize(1)
        val mission = prm.mission(0)
        rooms.vsRobot(ws.id, mission)
    }

    fun akt(ws: Ws, prm: Prm) {
        ensureLogin(ws, "createMission")
        rooms.akt(ws.id, prm)
    }

    fun land(ws: Ws, prm: Prm) {
        ensureLogin(ws, "land")
        prm.ensureEmpty()
        println(rooms.land(ws.id))
    }

    fun accept(ws: Ws, prm: Prm) {
        ensureLogin(ws, "matchAccept")
        prm.ensureEmpty()
        rooms.accept(ws.id)
    }
    fun invite(ws: Ws, prm: Prm) {
        ensureLogin(ws, "invite")
        prm.ensureSize(2)
        rooms.invite(ws.id, Id(prm.str(0, 4, 4)), prm.bet(1))
    }
    fun decline(ws: Ws, prm: Prm) {
        ensureLogin(ws, "decline")
        prm.ensureEmpty()
        rooms.decline(ws.id)
    }

    fun refresh(ws: Ws, prm: Prm) {
        ensureLogin(ws, "refresh")
        prm.ensureEmpty()
        rooms.refresh(ws.id)
    }

    fun ensureLogin(ws: Ws, cmd: String) {
        if (!ws.isLogin) throw Violation("need login for $cmd")
    }

    fun ensureAdmin(ws: Ws, cmd: String) {
        if (!ws.isLogin || ws.id.toString() != "0000") throw Violation("need admin for $cmd")
    }

    fun ensureUnlogin(ws: Ws, cmd: String) {
        if (ws.isLogin) throw Violation("need unlogin for ${cmd}")
    }

    fun sendUser(ws: Ws) {
        val user = users[ws.id]!!
        val obj = JSONObject()
        obj["id"] = user.id.toString()
        obj["nick"] = user.nick
        obj["balance"] = user.balance
        ws.send("u"+obj)
    }

    override fun invoke(id: Id, msg: String) {
        val ws = wss[id]
        if(ws==null) throw Err("User $id disconnected")
        ws.send(msg)
    }
}

abstract class Ws {
    private var _id: Id? = null

    val id: Id
        get() = _id!!

    val isLogin: Boolean
        get() = _id != null

    fun login(id: Id) {
        if (isLogin) throw Err("already isLogin")
        _id = id
    }

    abstract fun send(msg: String)
    abstract fun close()
    abstract fun ip():String
}

interface Sender{
    fun invoke(id:Id,msg:String)
}

// нарушение клиентом протокола, приводит к разрыву соединения с этим клиентом
class Violation(msg: String) : Exception(msg)

// любая ошибка
class Err(msg: String) : Exception(msg)