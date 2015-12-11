package unitcraft.server

import org.json.simple.JSONObject
import unitcraft.inject.inject
import java.time.Duration
import java.time.Instant
import java.util.*

// управление жц партии, оповещение клиентов об измениях в игре, управление контролем времени в игре
class Bttler {
    val log: Log by inject()
    val cmder: CmderGame by inject()

    fun start(bttl: Bttl, mission: Int?, canEdit: Boolean): Chain {
        bttl.data = cmder.createData(mission, canEdit)
        bttl.state = cmder.reset()
        switchClock(bttl)
        return sendGame(bttl, false)
    }

    fun cmd(bttl: Bttl, id: Id, prm: Prm): Chain {
        ensureBttlNoWin(bttl)
        val sideTimeout = updateClock(bttl)
        val (side, akt) = if (sideTimeout != null) {
            sideTimeout to "u"
        } else {
            val (version, aktPrm) = prm.akt()
            if (version != bttl.cmds.size) {
                log.outSync()
                return refresh(bttl, id)
            }
            bttl.sides[id]!! to aktPrm
        }
        return doCmd(bttl, side, akt)
    }

    private fun doCmd(bttl: Bttl, side: Side, akt: String): Chain {
        val chain = Chain()
        chain.addChain(aktAndSend(bttl, side, akt))
        if (bttl.isVsRobot && bttl.state.sideWin == null) {
            var cnt = 0
            while (true) {
                val cmdRobot = try {
                    cmder.cmdRobot(bttl.sideRobot()!!)
                } catch(e: Throwable) {
                    log.error(e)
                    "e"
                } ?: break
                chain.addChain(aktAndSend(bttl, bttl.sideRobot()!!, cmdRobot))
                cnt += 1
                if (bttl.state.sideWin != null || cnt >= 100) break
            }
        }
        return chain
    }

    fun surr(bttl: Bttl, id: Id): Chain {
        ensureBttlNoWin(bttl)
        val sideTimeout = updateClock(bttl)
        return doCmd(bttl, sideTimeout ?: bttl.sides[id]!!, "u")
    }

    fun timeout(bttl: Bttl, id: Id): Chain {
        ensureBttlNoWin(bttl)
        val sideTimeout = updateClock(bttl) ?: throw Err("too early timeout: " + bttl.clocks)
        return doCmd(bttl, sideTimeout, "u")
    }

    fun refresh(bttl: Bttl, id: Id) = sendGame(bttl, false, id)

    fun land() {
        cmder.land()
    }

    private fun ensureBttlNoWin(bttl: Bttl) {
        if (bttl.state.sideWin != null) throw Err("bttl ${bttl.id} is over")
    }

    private fun aktAndSend(bttl: Bttl, side: Side, akt: String): Chain {
        val isErr = try {
            bttl.state = cmder.cmd(side, akt)
            bttl.cmds.add(Pair(side, akt))
            bttl.state.swapSide?.let {
                if (it == SwapSide.usual || bttl.isVsRobot) bttl.swapSide()
            }
            switchClock(bttl)
            false
        } catch (ex: Violation) {
            throw ex
        } catch(ex: Throwable) {
            log.error(ex)
            resetGame(bttl)
            true
        }
        return sendGame(bttl, isErr)
    }

    private fun resetGame(bttl: Bttl) {
        cmder.reset()
        for ((side, akt) in bttl.cmds) {
            cmder.cmd(side, akt)
        }
    }

    private fun sendGame(bttl: Bttl, isErr: Boolean, idOnly: Id? = null): Chain {
        val chain = Chain()
        for ((id, side) in bttl.sides) if (idOnly == null || id == idOnly) {
            val json = bttl.state.json[side]!!
            json["version"] = bttl.cmds.size
            if (bttl.isVsRobot) json["isVsRobot"] = true
            else {
                json["bet"] = bttl.bet
                val clocks = listOf(side, side.vs).map { bttl.clocks[bttl.idBySide(it)!!]!! }
                json["clock"] = clocks.map { it.left.toMillis() }
                json["clockIsOn"] = clocks.map { !it.stoped() }
            }
            if (isErr) json["err"] = true else json.remove("err")
            chain.add(id, "g" + json)
        }
        return chain
    }

    private fun updateClock(bttl: Bttl): Side? {
        val now = Instant.now()
        bttl.clocks.values.forEach { it.update(now) }
        return if (bttl.clocks.values.all { it.elapsed() }) Side.a
        else bttl.clocks.entries.firstOrNull { it.value.elapsed() }?.key?.let { bttl.sides[it] }
    }

    private fun switchClock(bttl: Bttl) {
        if (bttl.isVsRobot) return
        if (bttl.state.sideWin != null) {
            bttl.clocks.values.forEach { it.stop() }
            return
        }
        val idClockStop = bttl.idBySide(bttl.state.sideClockStop)
        val now = Instant.now()
        if (idClockStop != null) {
            bttl.clocks[idClockStop]!!.stop()
            bttl.idBySide(bttl.state.sideClockStop?.vs)?.let { bttl.clocks[it] }?.let {
                if (it.stoped()) {
                    it.extend()
                    it.start(now)
                }
            }
        } else bttl.clocks.values.forEach { it.start(now) }
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
// [sideClockStop] - чей таймер должен быть остановлен, null - оба таймеры запущены
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

    val isVsRobot = idSec == null

    val sides = HashMap<Id, Side>().apply {
        this[idPrim] = Side.a
        if (idSec != null) this[idSec] = Side.b
    }

    val clocks = sides.mapValues { Clock(Duration.ofMinutes(2), Duration.ofMinutes(2)) }

    val cmds = ArrayList<Pair<Side, String>>()

    fun idBySide(side: Side?) = if (side != null) sides.entries.firstOrNull() { it.value == side }?.key else null

    fun swapSide() {
        sides[idPrim] = sides[idPrim]!!.vs
        if (idSec != null) sides[idSec] = sides[idSec]!!.vs
    }

    fun sideRobot() = if (isVsRobot) sides[idPrim]!!.vs else null

    fun idWin() = if (isVsRobot) null else state.sideWin?.let { idBySide(it) }
}

interface GameData