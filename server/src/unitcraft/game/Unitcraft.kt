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
    val stager = Stager(byGame { Score() })
    val editor = Editor(objs)
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

    init {
        val stazis = Stazis(r, stager,editor,drawer,byGame { Grid<Int>() })

        val drawerObjOwn = DrawerObjOwn(drawer,objs)
        val editorObjOwn = EditorObjOwn(editor,objs)

        val hider = Hider()
        val lifer = Lifer()
        val drawerVoin = DrawerVoin(r,drawer, objs)
        val enforcer = Enforcer(r,stager,drawerVoin,objs)
        val editorVoin = EditorVoin(editor,hider,lifer, objs)
        val pointControl = PointControl(stager,objs)

        Mine(r, drawerObjOwn,editorObjOwn,pointControl)
        Hospital(r, drawerObjOwn,editorObjOwn,pointControl)
        Flag(r, drawerObjOwn,editorObjOwn,pointControl)

        Electric(r, drawerVoin, editorVoin,spoter)
        Telepath(r, enforcer,drawerVoin, editorVoin)
        Staziser(r, stazis, drawerVoin, editorVoin,spoter)
        Inviser(r, stager,hider,drawerVoin, editorVoin,objs)
        Imitator(r, spoter,drawerVoin, editorVoin)
        Catapult(r, drawer, editor,objs)

        editorObjOwn.build()
        editorVoin.build()
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
                    spoter = spoter
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