package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.inject.inject
import unitcraft.inject.register
import unitcraft.land.Land
import unitcraft.server.*

fun registerUnitcraft(data: ()->DataUnitcraft = {DataUnitcraft(0,false)}): Resource {
    register(CmderUnitcraft())

    register(data)
    register({ data().allData })
    register({ data().allData.objs })
    register({ data().allData.flats })

    val r = Resource()

    register(Stager(r))
    register(Editor())
    register(Drawer())
    register(Spoter())
    register(Flater())
    register(Sider())
    register(Tracer(r))
    register(Mover())
    register(Stazis(r))
    register(Lifer(r))
    register(Enforcer(r))
    register(SkilerMove(r))
    register(SkilerHit(r))
    register(Builder(r))
    register(Solider(r))

    Forest(r)
    Grass(r)
    Water(r)
    Sand(r)
    Catapult(r)

    Mine(r)
    Hospital(r)
    Flag(r)

    //Electric(r)
    Telepath(r)
    Staziser(r)
    Inviser(r)
    //Imitator(r)
    Redeployer(r)
    Warehouse(r)
    return r
}

class DataUnitcraft(mission: Int?, val canEdit: Boolean) {
    val land = Land(mission)
    lateinit var allData: AllData
}

class CmderUnitcraft : CmderGame {
    val data: () -> DataUnitcraft by inject()

    val flater: Flater by inject()
    val solider: Solider by inject()
    val editor: Editor by inject()
    val stager: Stager by inject()
    val spoter: Spoter by inject()
    val drawer: Drawer by inject()
    val tracer: Tracer by inject()

    override fun reset() {
        data().allData = AllData()
        flater.reset(data().land.flats)
        solider.reset(data().land.solids)
    }

    override fun cmd(side: Side, cmd: String) {
        if (side.isN) throw throw Err("side is neutral")
        if (cmd.isEmpty()) throw Violation("cmd is empty")
        val prm = Prm(data().land.pgser, cmd[1, cmd.length()].toString())
        when (cmd[0]) {
            'z' -> editAdd(side, prm)
            'r' -> editRemove(prm)
            'd' -> editDestroy(prm)
            'c' -> editChange(side, prm)
            'a' -> akt(side, prm)
            'b' -> aktOpt(side, prm)
            'e', 'w' -> endTurn(side, prm)
            else -> throw Violation("unknown msg: " + cmd)
        }
    }

    override fun state(): GameState {
        return GameState(null, Side.ab.map { it to snap(it).toJson() }.toMap(), null)
    }

    override fun cmdRobot(sideRobot: Side): String? {
        return if (stager.sideTurn() == sideRobot) "e" else null
    }

    override fun land(): String {
        throw UnsupportedOperationException()
    }

    private fun editAdd(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(3)
        val num = prm.int(2)
        if (num >= editor.opterTest.opts.size()) throw Violation("editAdd out bound")
        editor.editAdd(prm.pg(0), side, num)
    }

    private fun editRemove(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        editor.editRemove(prm.pg(0))
    }

    private fun editDestroy(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        editor.editDestroy(prm.pg(0))
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        solider.editChange(prm.pg(0), side)
    }

    private fun akt(side: Side, prm: Prm) {
        prm.ensureSize(5)
        spoter.akt(side, prm.pg(0), prm.int(2), prm.pg(3))
    }

    private fun aktOpt(side: Side, prm: Prm) {
        prm.ensureSize(6)
        spoter.akt(side, prm.pg(0), prm.int(2), prm.pg(3), prm.int(5))
    }

    private fun endTurn(side: Side, prm: Prm) {
        prm.ensureSize(0)
        if (stager.sideTurn() != side) throw Violation("endTurn side($side) != sideTurn")
        stager.endTurn()
    }

    private fun ensureTest() {
        if (!data().canEdit) throw Violation("only for test game")
    }

    private fun snap(side: Side) = Snap(
            data().land.pgser.xr,
            data().land.pgser.yr,
            drawer.draw(side),
            spoter.spots(side),
            tracer.traces(side), stager.stage(side), stager.edge(side), stager.focus, if (data().canEdit) editor.opterTest else null
    )
}