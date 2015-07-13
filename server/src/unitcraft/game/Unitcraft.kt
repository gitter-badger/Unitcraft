package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.game.rule.Inviser
import unitcraft.land.Land
import unitcraft.server.*
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.properties.Delegates

class Unitcraft(r: Resource = Resource()) : CreatorGame {
    override fun createGame(mission: Int?) = CmderUnitcraft(mission,true)

    private var cur: WeakReference<Any> by Delegates.notNull()

    private fun byGame<T : Any>(obj: () -> T): () -> T {
        val map = WeakHashMap<Any, T>()
        return { map.getOrPut(cur.get(), obj) }
    }

    val pgser = { cur.get()!!.pgser }
    val objs = byGame{Objs()}
    val stager = Stager(objs)
    val editor = Editor()
    val drawer = Drawer(objs)
    val spoter = Spoter(stager,objs)

    val gridPlace = byGame { Grid<TpPlace>() }
    val sizeFix: Map<TpPlace, Int> = mapOf(
            TpPlace.forest to 4,
            TpPlace.grass to 5,
            TpPlace.hill to 1,
            TpPlace.mount to 1,
            TpPlace.sand to 4,
            TpPlace.water to 1
    )
    val tilesPlace = TpPlace.values().map { it to r.tlsList(sizeFix[it]!!, it.name(), Resource.effectPlace) }.toMap()
    val place = Place(pgser, tilesPlace, gridPlace, byGame { Grid<Map<TpPlace, Int>>() },drawer,editor)

    val sider = Sider(spoter,objs)
    val tracer = Tracer(r)

    init {
        val hider = Hider()
        val shaper = Shaper(r,hider,editor,objs)
        val stazis = Stazis(r, stager,editor,drawer,spoter, shaper,byGame { Grid<Int>() })

        Catapult(r, drawer, spoter,shaper,objs)

        val drawerPointControl = DrawerPointControl(drawer,sider,objs)
        val pointControl = PointControl(r,stager,sider,drawerPointControl,shaper,objs)

        val drawerVoin = DrawerVoin(r,drawer, hider,sider,spoter,objs)
        val lifer = Lifer(r,drawerVoin,shaper)
        val enforcer = Enforcer(r,stager,drawerVoin,spoter,objs)
        val skilerMove = SkilerMove(r,spoter,shaper)
        val builder = Builder(r,lifer,sider,spoter, shaper,objs)
        val voiner = Voiner(r,hider,drawerVoin, shaper, sider, lifer, enforcer,spoter,pointControl, builder,skilerMove)

        Mine(pointControl,stager,sider,builder,objs)
        Hospital(pointControl)
        Flag(pointControl)

        Electric(r, voiner)
        Telepath(r, enforcer,voiner,spoter)
        Staziser(r, stazis,voiner,spoter)
        Inviser(voiner, hider, sider,  stager,objs)
        Imitator(spoter,voiner,objs)
        Redeployer(voiner,builder)
        Warehouse(voiner,builder, lifer)
    }

    inner class CmderUnitcraft(mission: Int?,val canEdit:Boolean) : CmderGame {
        val land = Land(mission, sizeFix)
        val pgser = land.pgser

        var game:Any by Delegates.notNull()


        init {
            reset()
        }

        override fun reset() {
            game = Any()
            cur = WeakReference(game)
            for ((pg, v) in land.grid()) place.grid().set(pg, v)
            for ((pg, v) in land.fixs()) place.fixs().set(pg, v)
        }

        override fun cmd(side: Side, cmd: String) {
            cur = WeakReference(game)
            if(side.isN) throw throw Err("side is neutral")
            if (cmd.isEmpty()) throw Violation("cmd is empty")
            val prm = Prm(pgser, cmd[1, cmd.length()].toString())
            when (cmd[0]) {
                'z' -> editAdd(side, prm)
                'r' -> editRemove(prm)
                'd' -> editDestroy(prm)
                'c' -> editChange(side, prm)
                'a' -> akt(side, prm)
                'b' -> aktOpt(side, prm)
                'e','w' -> endTurn(side, prm)
                else -> throw Violation("unknown msg: " + cmd)
            }
        }

        override fun state(): GameState {
            cur = WeakReference(game)
            return GameState(null, Side.values().map { it to snap(it).toJson() }.toMap(), null)
        }

        override fun cmdRobot(sideRobot:Side): String? {
            WeakReference(this)
            return if (stager.sideTurn() == sideRobot) "e" else null
        }

        override fun land(): String {
            throw UnsupportedOperationException()
        }

        private fun editAdd(side: Side, prm: Prm) {
            ensureTest()
            prm.ensureSize(3)
            val num = prm.int(2)
            if(num >= editor.opterTest.opts.size()) throw Violation("editAdd out bound")
            editor.editAdd(prm.pg(0),side,num)
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
            sider.editChange(prm.pg(0), side)
        }

        private fun akt(side: Side, prm: Prm) {
            prm.ensureSize(5)
            spoter.akt(side,prm.pg(0),prm.int(2),prm.pg(3))
        }

        private fun aktOpt(side: Side, prm: Prm) {
            prm.ensureSize(6)
            spoter.akt(side,prm.pg(0),prm.int(2),prm.pg(3),prm.int(5))
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
                tracer.traces(side), side == stager.sideTurn(), Stage.turn, if(canEdit) editor.opterTest else null
        )
    }
}