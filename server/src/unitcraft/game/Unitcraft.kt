package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.land.Land
import unitcraft.land.TpFlat
import unitcraft.server.*
import kotlin.properties.Delegates

class Unitcraft(r: Resource = Resource()) : CreatorGame {
    override fun createGame(mission: Int?) = CmderUnitcraft(mission, true)

    private var cur: CmderUnitcraft by Delegates.notNull()

    val pgser = { cur.pgser }
    val allData = { cur.allData }
    val objs = { cur.allData.objs }
    val flats = { cur.allData.flats }

    val stager = Stager(allData)
    val editor = Editor()
    val hider = Hider()
    val drawer = Drawer(r, hider, pgser, allData)
    val spoter = Spoter(stager, allData)


    //    val sizeFix: Map<TpPlace, Int> = mapOf(
    //            TpPlace.forest to 4,
    //            TpPlace.grass to 5,
    //            TpPlace.hill to 1,
    //            TpPlace.mount to 1,
    //            TpPlace.sand to 4,
    //            TpPlace.water to 1
    //    )
    //val tilesPlace = TpPlace.values().map { it to r.tlsList(sizeFix[it]!!, it.name(), Resource.effectPlace) }.toMap()
    val flater:Flater = Flater(r, stager, allData, drawer, editor)

    val sider = Sider(spoter, objs)
    val tracer = Tracer(r)


    val shaper = Mover(r, hider, editor, objs)
    val stazis = Stazis(r, stager, editor, drawer, spoter, shaper, flats)

    val lifer = Lifer(r, drawer, shaper)
    val enforcer = Enforcer(r, stager, drawer, spoter, objs)
    val skilerMove = SkilerMove(r, spoter, shaper)
    val builder = Builder(r, lifer, sider, spoter, shaper, objs)
    val voiner = Solider(r, hider, drawer, editor, sider, lifer, enforcer, spoter, shaper, builder, skilerMove, objs)

    init {
        Forest(r, flater)
        Grass(r, flater)
        Water(r, flater)
        Catapult(r, flater, spoter, shaper, flats)

        Mine(r, flater, stager, builder, flats)
        Hospital(r, flater)
        Flag(r, flater)

        //        Electric(r, voiner)
        Telepath(r, enforcer, voiner, spoter)
        Staziser(r, stazis,voiner,spoter)
        //        Inviser(voiner, hider, sider,  stager,objs)
        //        Imitator(spoter,voiner,objs)
        //        Redeployer(voiner,builder)
        //        Warehouse(voiner,builder, lifer)
    }

    inner class CmderUnitcraft(mission: Int?, val canEdit: Boolean) : CmderGame {
        val land = Land(flater.landTps, mission)
        val pgser = land.pgser
        var allData: AllData by Delegates.notNull()


        init {
            reset()
        }

        override fun reset() {
            allData = AllData()
            for(pg in pgser){
                val flat = Flat(Singl(pg))
                land.flat(pg)(flat)
                allData.flats.add(flat)
            }
        }

        override fun cmd(side: Side, cmd: String) {
            cur = this
            if (side.isN) throw throw Err("side is neutral")
            if (cmd.isEmpty()) throw Violation("cmd is empty")
            val prm = Prm(pgser, cmd[1, cmd.length()].toString())
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
            cur = this
            return GameState(null, Side.values().map { it to snap(it).toJson() }.toMap(), null)
        }

        override fun cmdRobot(sideRobot: Side): String? {
            cur = this
            return if (stager.sideTurn() == sideRobot) "e" else null
        }

        override fun land(): String {
            cur = this
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
            editor.editChange(prm.pg(0), side)
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
            if (!canEdit) throw Violation("only for test game")
        }

        private fun snap(side: Side) = Snap(
                pgser.xr,
                pgser.yr,
                drawer.draw(side),
                spoter.spots(side),
                tracer.traces(side), side == stager.sideTurn(), Stage.turn, if (canEdit) editor.opterTest else null
        )
    }
}