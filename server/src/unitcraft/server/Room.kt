package unitcraft.server

import unitcraft.game.Game
import java.util.ArrayList
import org.json.simple.JSONObject
import java.time.Duration
import java.time.Instant

// управление жц игры, оповещение клиентов об измениях в игре, управление контролем времени в игре
// нужен контроль времени
class Room(val log: Log, val send: Sender, val idA: Id, val idB: Id?, val bet: Int = 0, private val cmder: CmderGame) {
    val sides = if (idB != null) mapOf(idA to Side.a, idB to Side.b) else mapOf(idA to Side.a)
    val ids = if (idB != null) mapOf(Side.a to idA, Side.b to idB) else mapOf(Side.a to idA)
    val cmds = ArrayList<Pair<Side, String>>()

    private var state:GameState

    init {
        state = cmder.state()
        sendGame(false)
    }

    val isVsRobot:Boolean
        get() = idB == null

    fun cmd(id: Id, prm: Prm) {
        val (version, akt) = prm.akt()
        if (version != cmds.size()) {
            log.outSync()
            refresh(id)
            return
        }
        aktAndSend(sides[id]!!, akt)
        if (isVsRobot && state.sideWin == null) {
            while (true) {
                val cmdRobot = try{
                    cmder.cmdRobot()
                }catch(e:Throwable){
                    log.error(e)
                    "e"
                } ?: break
                aktAndSend(Side.b, cmdRobot)
                if (state.sideWin != null) break
            }
        }
    }

    fun idWin(): Id? {
        if (isVsRobot) throw Err("call idWin() on robot room")
        return if (state.sideWin != null) ids[state.sideWin] else null
    }

    fun refresh(id: Id) {
        sendGame(false,id)
    }

    fun land(id: Id) {
        cmder.land()
    }

    private fun aktAndSend(side: Side, akt: String) {
        try {
            cmder.cmd(side, akt)
            state = cmder.state()
            cmds.add(Pair(side, akt))
            sendGame(false)
        } catch (ex: Violation) {
            throw ex
        } catch(ex: Throwable) {
            log.error(ex)
            resetGame()
            sendGame(true)
        }

    }

    private fun resetGame() {
        cmder.reset()
        for ((side, akt) in cmds) {
            cmder.cmd(side, akt)
        }
    }

    private fun sendGame(isErr: Boolean,idOnly:Id?=null) {
        for ((id,side) in sides) if(idOnly==null || id==idOnly){
            val json = state.json[side]!!
            json["version"] = cmds.size()
            json["bet"] = bet
            if (isErr) json["err"] = true else json.remove("err")
            send(id, "g" + json)
        }
    }
}

class Clock(private var left: Duration) {
    private var last: Instant? = null

    fun start(now: Instant) {
        if (started()) throw Err("clock already started")
        if (elapsed()) throw Err("clock already elapsed")
        last = now
    }

    fun stop() {
        left = leftNow()
        last = null
    }

    fun add(dur: Duration) {
        left += dur
    }

    fun elapsed(): Boolean {
        if (started()) {
            left = leftNow()
            last = Instant.now()
        }
        return left.isZero()
    }

    fun started() = last != null

    fun leftNow(): Duration {
        if (last != null) {
            val dur = left.minus(Duration.between(Instant.now(), last))!!
            return if (dur.isNegative()) Duration.ZERO else dur
        } else {
            return left
        }
    }
}

interface CmderGame {
    // сбрасывает состояние игры до исходного
    fun reset()

    // возвращает сторону победителя
    // если null, значит игра еще не окончена
    fun cmd(side: Side, cmd: String)

    // состояние игры
    fun state(): GameState

    // если не null, значит ИИ должен сходить
    fun cmdRobot(): String?

    // сохранить карту
    fun land(): String
}

// состояние игры
// определен ли победитель?
// json расположения юнитов
// чей таймер должны быть запущены
class GameState(val sideWin:Side?,val json:Map<Side,JSONObject>, val sideClockOn:Side?)

enum class Side {
    a, b, n;

    val vs:Side
       get() = when (this) {
        a -> b
        b -> a
        n -> n
    }

    val isN:Boolean
        get() = this==n
}
