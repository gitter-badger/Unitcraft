package unitcraft.server

import org.json.simple.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.ArrayList
import java.util.HashMap
import java.util.Random
import kotlin.properties.Delegates

// управление жц игры, оповещение клиентов об измениях в игре, управление контролем времени в игре
// TODO нужен контроль времени
class Room(val log: Log, val send: Sender, val idPrim: Id, val idSec: Id?, val bet: Int = 0, private val cmder: CmderGame) {
    private val sides = HashMap<Id, Side>().init {
        if (idSec != null) if (r.nextBoolean()) {
            this[idPrim] = Side.a
            this[idSec] = Side.b
        } else {
            this[idPrim] = Side.b
            this[idSec] = Side.a
        } else this[idPrim] = Side.a
    }

    val cmds = ArrayList<Pair<Side, String>>()

    private var state: GameState

    init {
        state = cmder.state()
        sendGame(false)
    }

    val isVsRobot = idSec == null

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
                val cmdRobot = try {
                    cmder.cmdRobot(sides[idPrim].vs)
                } catch(e: Throwable) {
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
        return if (state.sideWin != null) sides.entrySet().first { it.value == state.sideWin }.key else null
    }

    fun refresh(id: Id) {
        sendGame(false, id)
    }

    fun land(id: Id) {
        cmder.land()
    }

    private fun aktAndSend(side: Side, akt: String) {
        try {
            cmder.cmd(side, akt)
            state = cmder.state()
            cmds.add(Pair(side, akt))
            if (isVsRobot && akt == "w") sides[idPrim] = sides[idPrim].vs
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

    private fun sendGame(isErr: Boolean, idOnly: Id? = null) {
        for ((id, side) in sides) if (idOnly == null || id == idOnly) {
            val json = state.json[side]!!
            json["version"] = cmds.size()
            json["bet"] = bet
            json["clock"] = listOf(1000000, 198000)
            if (isErr) json["err"] = true else json.remove("err")
            send(id, "g" + json)
        }
    }

    companion object {
        val r = Random()
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
    fun cmdRobot(sideRobot: Side): String?

    // сохранить карту
    fun land(): String
}

// состояние игры
// определен ли победитель?
// json расположения юнитов
// [sideClockOn] - чей таймер должны быть запущен, null - оба таймеры запущены
class GameState(val sideWin: Side?, val json: Map<Side, JSONObject>, val sideClockOn: Side?)

enum class Side {
    a, b, n;

    val vs: Side by Delegates.lazy {
        when (this) {
            a -> b
            b -> a
            n -> n
        }
    }

    val isN: Boolean by Delegates.lazy { this == n }
    companion object{
        val ab = listOf(Side.a,Side.b).requireNoNulls()
    }
}
