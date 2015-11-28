package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.inject.inject
import unitcraft.inject.register
import unitcraft.land.Land
import unitcraft.server.*

object FncUnitcraft {
    lateinit var data: () -> DataUnitcraft
    lateinit var allData: () -> AllData
    lateinit var objs: () -> Objs
    lateinit var flats: () -> Flats
}

fun injectData() = lazy(LazyThreadSafetyMode.NONE) { FncUnitcraft.data }
fun injectAllData() = lazy(LazyThreadSafetyMode.NONE) { FncUnitcraft.allData }
fun injectObjs() = lazy(LazyThreadSafetyMode.NONE) { FncUnitcraft.objs }
fun injectFlats() = lazy(LazyThreadSafetyMode.NONE) { FncUnitcraft.flats }

fun registerUnitcraft(data: () -> GameData = { object : GameData {} }): Resource {
    register<CmderGame>(CmderUnitcraft())

    FncUnitcraft.data = { data() as DataUnitcraft }
    FncUnitcraft.allData = { FncUnitcraft.data().allData }
    FncUnitcraft.objs = { FncUnitcraft.data().allData.objs }
    FncUnitcraft.flats = { FncUnitcraft.data().allData.flats }

    val r = Resource()

    register(Stager(r))
    register(Editor())
    register(Drawer(r))
    register(Spoter(r))
    register(Flater(r))
    register(Sider())
    register(Mover(r))
    register(Lifer(r))
    register(Stazis(r))
    register(Enforcer(r))
    register(SkilerMove(r))
    register(SkilerHit(r))
    register(Builder(r))
    register(Solider(r))
    register(Tracer(r))

    Forest(r)
    Grass(r)
    Water(r)
    Sand(r)
    Catapult(r)
    Fortress(r)
    Mine(r)
    Hospital(r)
    register(Flag(r))

    Redeployer(r)
    Armorer(r)
    Airport(r)
    Inviser(r)

    Electric(r)
    Telepath(r)
    Staziser(r)
    Imitator(r)
    Frog(r)
    Mina(r)
    Kicker(r)
    Jumper(r)

    return r
}

class DataUnitcraft(mission: Int?, val canEdit: Boolean) : GameData {
    val land = Land(mission)
    lateinit var allData: AllData
}

class CmderUnitcraft : CmderGame {
    val data: () -> DataUnitcraft by injectData()

    val flater: Flater by inject()
    val solider: Solider by inject()
    val builder: Builder by inject()
    val editor: Editor by inject()
    val stager: Stager by inject()
    val spoter: Spoter by inject()
    val drawer: Drawer by inject()
    val tracer: Tracer by inject()
    val lifer: Lifer by inject()

    override fun createData(mission: Int?, canEdit: Boolean) = DataUnitcraft(mission, canEdit)

    override fun reset(): GameState {
        data().allData = AllData()
        flater.reset(data().land.flats)
        solider.reset(data().land.solids)
        return state()
    }

    override fun cmd(side: Side, cmd: String): GameState {
        if (side.isN) throw throw Err("side is neutral")
        if (cmd.isEmpty()) throw Violation("cmd is empty")
        val prm = Prm(data().land.pgser, cmd[1, cmd.length].toString())
        var swapSide: SwapSide? = null
        when (cmd[0]) {
            's' -> selectBonus(side, prm)
            'j' -> swapSide = if (join(side, prm)) SwapSide.usual else null
            'z' -> editAdd(side, prm)
            'r' -> editRemove(prm)
            'd' -> editDestroy(prm)
            'c' -> editChange(side, prm)
            'a' -> akt(side, prm)
            'b' -> aktOpt(side, prm)
            'e' -> endTurn(side, prm)
            'w' -> {
                endTurn(side, prm)
                swapSide = SwapSide.ifRobot
            }
            else -> throw Violation("unknown msg: " + cmd)
        }
        return state(swapSide)
    }

    private fun state(swapSide: SwapSide? = null) =
            GameState(data().allData.sideWin, Side.ab.map { it to snap(it).toJson() }.toMap(), sideClockStop(), swapSide)

    private fun sideClockStop() = if (allData().bonus.isEmpty()) null
    else if (allData().bonus.size == 1) allData().bonus.keys.first()
    else stager.sideTurn().vs

    override fun cmdRobot(sideRobot: Side) = when (stager.stage(sideRobot)) {
        Stage.bonus -> "s0"
        Stage.turn -> "e"
        else -> null
    }

    override fun land(): String {
        throw UnsupportedOperationException()
    }

    private fun selectBonus(side: Side, prm: Prm) {
        if (stager.stage(side) != Stage.bonus) throw Violation("stage != bonus")
        prm.ensureSize(1)
        val bonus = prm.int(0)
        if (bonus > 50) throw Violation("bonus $bonus too high")
        allData().bonus[side] = bonus
        allData().bonus[side.vs]?.let { allData().sideTurn = if (it >= bonus) side.vs else side }
    }

    private fun join(side: Side, prm: Prm): Boolean {
        if (stager.stage(side) != Stage.join) throw Violation("stage != join")
        prm.ensureSize(1)
        val num = prm.int(0)
        if (num > 1) throw Violation("num($num) must be 0 or 1")
        allData().needJoin = false
        val sideJoined = Side.ab[num]
        builder.plusGold(sideJoined.vs, allData().bonus[side]!!)
        allData().sideTurn = sideJoined
        return side != sideJoined
    }

    private fun editAdd(side: Side, prm: Prm) {
        ensureCanEdit()
        prm.ensureSize(3)
        tracer.clear()
        val num = prm.int(2)
        if (num >= editor.opterTest.opts.size) throw Violation("editAdd out bound")
        editor.editAdd(prm.pg(0), side, num)
    }

    private fun editRemove(prm: Prm) {
        ensureCanEdit()
        prm.ensureSize(2)
        tracer.clear()
        editor.editRemove(prm.pg(0))
    }

    private fun editDestroy(prm: Prm) {
        ensureCanEdit()
        prm.ensureSize(2)
        tracer.clear()
        lifer.damage(prm.pg(0),1)
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureCanEdit()
        prm.ensureSize(2)
        tracer.clear()
        solider.editChange(prm.pg(0), side)
    }

    private fun akt(side: Side, prm: Prm) {
        if (!stager.isTurn(side)) throw Violation("not your turn")
        prm.ensureSize(5)
        tracer.clear()
        spoter.akt(side, prm.pg(0), prm.int(2), prm.pg(3))
    }

    private fun aktOpt(side: Side, prm: Prm) {
        if (!stager.isTurn(side)) throw Violation("not your turn")
        prm.ensureSize(6)
        tracer.clear()
        spoter.akt(side, prm.pg(0), prm.int(2), prm.pg(3), prm.int(5))
    }

    private fun endTurn(side: Side, prm: Prm) {
        if (!stager.isTurn(side)) throw Violation("not your turn")
        prm.ensureSize(0)
        tracer.clear()
        stager.endTurn()
    }

    private fun ensureCanEdit() {
        if (!data().canEdit) throw Violation("only for canEdit game")
    }

    private fun allData() = data().allData

    private fun snap(side: Side) = Snap(
            data().land.pgser.xr,
            data().land.pgser.yr,
            drawer.draw(side),
            spoter.spots(side),
            allData().traces[side]!!,
            stager.stage(side),
            stager.edge(side),
            stager.focus,
            spoter.pgFocus(),
            listOf(allData().point[side]!!, allData().point[side.vs]!!),
            if (data().canEdit) editor.opterTest else null
    )
}