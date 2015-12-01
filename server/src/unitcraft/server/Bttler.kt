package unitcraft.server

import org.json.simple.JSONObject
import unitcraft.inject.inject
import java.time.Duration
import java.time.Instant
import java.util.*

// управление жц игры, оповещение клиентов об измениях в игре, управление контролем времени в игре
// TODO нужен контроль времени
class Bttler {
    val log: Log by inject()
    val cmder: CmderGame by inject()
    val bttl: () -> Bttl by injectBttl()

    fun start(mission: Int?, canEdit: Boolean): Chain {
        bttl().data = cmder.createData(mission, canEdit)
        bttl().state = cmder.reset()
        updateClock()
        return sendGame(false)
    }

    fun isVsRobot() = bttl().sideRobot() != null

    fun cmd(id: Id, prm: Prm): Chain {
        val (version, akt) = prm.akt()
        if (version != bttl().cmds.size) {
            log.outSync()
            return refresh(id)
        }
        val chain = Chain()
        chain.addChain(aktAndSend(bttl().sides[id]!!, akt))
        if (isVsRobot() && bttl().state.sideWin == null) {
            while (true) {
                val cmdRobot = try {
                    cmder.cmdRobot(bttl().sideRobot()!!)
                } catch(e: Throwable) {
                    log.error(e)
                    "e"
                } ?: break
                chain.addChain(aktAndSend(bttl().sideRobot()!!, cmdRobot))
                if (bttl().state.sideWin != null) break
            }
        }
        return chain
    }

    fun refresh(id: Id): Chain = sendGame(false, id)

    fun land(id: Id) {
        cmder.land()
    }

    private fun aktAndSend(side: Side, akt: String): Chain {
        try {
            bttl().state = cmder.cmd(side, akt)
            bttl().cmds.add(Pair(side, akt))
            bttl().state.swapSide?.let {
                if (it == SwapSide.usual || isVsRobot()) bttl().swapSide()
            }
            updateClock()
            return sendGame(false)
        } catch (ex: Violation) {
            throw ex
        } catch(ex: Throwable) {
            log.error(ex)
            resetGame()
            return sendGame(true)
        }
    }

    private fun resetGame() {
        cmder.reset()
        for ((side, akt) in bttl().cmds) {
            cmder.cmd(side, akt)
        }
    }

    private fun sendGame(isErr: Boolean, idOnly: Id? = null): Chain {
        val bttl = bttl()
        val chain = Chain()
        for ((id, side) in bttl.sides) if (idOnly == null || id == idOnly) {
            val json = bttl.state.json[side]!!
            json["version"] = bttl.cmds.size
            json["bet"] = bttl.bet
            json["clock"] = listOf(side, side.vs).map { bttl().clocks[it]!!.leftNow().toMillis() }
            json["clockIsOn"] = listOf(side, side.vs).map { bttl().clocks[it]!!.started() }
            if (isErr) json["err"] = true else json.remove("err")
            chain.add(id, "g" + json)
        }
        return chain
    }

    private fun updateClock() {
        if (isVsRobot()) return
        val sideClockStop = bttl().state.sideClockStop
        if (sideClockStop != null) {
            bttl().clocks[sideClockStop]!!.stop()
            val clockVs = bttl().clocks[sideClockStop.vs]!!
            if(!clockVs.started()){
                clockVs.extend()
                clockVs.start(Instant.now())
            }
        }else {
            val now = Instant.now()
            bttl().clocks.values.forEach { it.start(now) }
        }
        if (bttl().idWin() != null) bttl().clocks.values.forEach { it.stop() }

    }
}

class Chain() {

    constructor(id: Id, json: String) : this() {
        add(id, json)
    }

    val list = ArrayList<Pair<Id, String>>()

    fun add(id: Id, json: String) {
        list.add(id to json)
    }

    fun addChain(chain: Chain) {
        list.addAll(chain.list)
    }
}

class Clock {
    private var left = Duration.ofMinutes(3)
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

    fun extend() {
        left += Duration.ofMinutes(2)
    }

    fun elapsed(): Boolean {
        if (started()) {
            left = leftNow()
            last = Instant.now()
        }
        return left.isZero
    }

    fun started() = last != null

    fun leftNow(): Duration {
        if (last != null) {
            val dur = left.minus(Duration.between(last,Instant.now()))!!
            return if (dur.isNegative) Duration.ZERO else dur
        } else {
            return left
        }
    }
}

interface CmderGame {
    // начинает партию и создает ее data
    fun createData(mission: Int?, canEdit: Boolean): GameData

    // сбрасывает состояние игры до исходного
    fun reset(): GameState

    // возвращает сторону победителя
    // если null, значит игра еще не окончена
    fun cmd(side: Side, cmd: String): GameState

    // если не null, значит ИИ должен сходить
    fun cmdRobot(sideRobot: Side): String?

    // сохранить карту
    fun land(): String
}

// состояние игры
// определен ли победитель?
// json расположения юнитов
// [sideClockOn] - чей таймер должны быть запущен, null - оба таймеры запущены
class GameState(val sideWin: Side?, val json: Map<Side, JSONObject>, val sideClockStop: Side?, val swapSide: SwapSide?)

enum class SwapSide {
    usual, ifRobot
}

enum class Side {
    a, b;

    val vs: Side by lzy {
        when (this) {
            a -> b
            b -> a
        }
    }

    companion object {
        val ab = listOf(Side.a, Side.b)
    }
}

class Bttl(val idPrim: Id, val idSec: Id? = null, val bet: Int = 0) {
    lateinit var data: GameData
    lateinit var state: GameState

    val id = "$idPrim-${idSec ?: "AI"}-${Instant.now()}"

    val sides = HashMap<Id, Side>().apply {
        if (idSec != null) if (r.nextBoolean()) {
            this[idPrim] = Side.a
            this[idSec] = Side.b
        } else {
            this[idPrim] = Side.b
            this[idSec] = Side.a
        } else this[idPrim] = Side.a
    }

    val clocks = mapOf(Side.a to Clock(), Side.b to Clock())

    companion object {
        val r = Random()
    }

    val cmds = ArrayList<Pair<Side, String>>()


    fun swapSide() {
        sides[idPrim] = sides[idPrim]!!.vs
        if (idSec != null) sides[idSec] = sides[idSec]!!.vs
    }

    fun idWin(): Id? =
            if (idSec == null) null
            else if (state.sideWin != null) sides.entries.first { it.value == state.sideWin }.key
            else null

    fun sideRobot() = if (idSec == null) sides[idPrim]!!.vs else null

}

interface GameData