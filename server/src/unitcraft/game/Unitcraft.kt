package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.game.rule.Inviser
import unitcraft.land.Land
import unitcraft.server.CmderGame
import unitcraft.server.CreatorGame
import unitcraft.server.GameState
import unitcraft.server.Side
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.properties.Delegates

class Unitcraft(r: Resource = Resource()) : CreatorGame {
    override fun createGame(mission: Int?) = CmderUnitcraft(mission)

    private var gameCur: WeakReference<Game> by Delegates.notNull()

    private fun byGame<T : Any>(obj: () -> T): () -> T {
        val map = WeakHashMap<Game, T>()
        return { map.getOrPut(gameCur.get(), obj) }
    }

    val pgser = { gameCur.get()!!.pgser }
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

    init {
        val hider = Hider()
        val shaper = Shaper(r,hider,objs)
        val stazis = Stazis(r, stager,editor,drawer,shaper,byGame { Grid<Int>() })

        Catapult(r, drawer, editor,spoter,shaper,objs)

        val drawerPointControl = DrawerPointControl(drawer,sider,objs)
        val editorPointControl = EditorPointControl(editor,shaper,sider,objs)
        val pointControl = PointControl(r,stager,sider,drawerPointControl,editorPointControl,objs)

        Mine(pointControl)
        Hospital(pointControl)
        Flag(pointControl)

        val drawerVoin = DrawerVoin(r,drawer, hider,sider,spoter,objs)
        val editorVoin = EditorVoin(editor,shaper,sider,spoter, objs)
        val lifer = Lifer(r,drawerVoin,shaper)
        val enforcer = Enforcer(r,stager,drawerVoin,spoter,objs)
        val skilerMove = SkilerMove(r,spoter,shaper)
        val voiner = Voiner(r,hider,drawerVoin, editorVoin, sider, lifer, enforcer,skilerMove)

        Electric(r, voiner)
        Telepath(r, enforcer,voiner)
        Staziser(r, stazis,voiner,spoter)
        Inviser(voiner, hider, sider,  stager,objs)
        Imitator(spoter,voiner)
    }

    inner class CmderUnitcraft(mission: Int?) : CmderGame {
        val land = Land(mission, sizeFix)
        val pgser = land.pgser

        var game: Game by Delegates.notNull()


        init {
            reset()
        }

        override fun reset() {
            game = Game(
                    pgser = pgser,
                    drawer = drawer,
                    editor = editor,
                    stager = stager,
                    spoter = spoter,
                    sider = sider
            )
            gameCur = WeakReference(game)
            for ((pg, v) in land.grid()) place.grid().set(pg, v)
            for ((pg, v) in land.fixs()) place.fixs().set(pg, v)
        }

        override fun cmd(side: Side, cmd: String) {
            gameCur = WeakReference(game)
            game.cmd(side, cmd)
        }

        override fun state(): GameState {
            gameCur = WeakReference(game)
            return game.state()
        }

        override fun cmdRobot(): String? {
            WeakReference(game)
            return game.cmdRobot()
        }

        override fun land(): String {
            throw UnsupportedOperationException()
        }
    }
}