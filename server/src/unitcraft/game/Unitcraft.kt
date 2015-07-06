package unitcraft.game

import unitcraft.land.Land
import unitcraft.server.CreatorGame
import unitcraft.server.CmderGame
import unitcraft.game.Game
import unitcraft.game.rule.*
import unitcraft.server.GameState
import unitcraft.server.Side
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates

class Unitcraft(r:Resource = Resource()) : CreatorGame {
    override fun createGame(mission:Int?) = CmderUnitcraft(mission)

    var gameCur:WeakReference<Game> by Delegates.notNull()

    fun byGame<T:Any>(obj:()->T):()->T{
        val map = WeakHashMap<Game,T>()
        return {map.getOrPut(gameCur.get(),obj)}
    }

    val sizeFix: Map<TpPlace, Int> = mapOf(
            TpPlace.forest to 4,
            TpPlace.grass to 5,
            TpPlace.hill to 1,
            TpPlace.mount to 1,
            TpPlace.sand to 4,
            TpPlace.water to 1
    )

    val pgser = {gameCur.get()!!.pgser}
    val gridPlace = byGame{Grid<TpPlace>()}

    val tilesPlace = TpPlace.values().map { it to r.tlsList(sizeFix[it]!!, it.name(), Resource.effectPlace) }.toMap()
    val placeEdits = TpPlace.values().map{PlaceEdit(it,tilesPlace[it]!!.last(),gridPlace)}
    val place = Place(pgser,tilesPlace,gridPlace,byGame{Grid<Map<TpPlace,Int>>()})

    val resDrawSimple = ResDrawSimple(r)

    val exts:List<Ext> = placeEdits + listOf(
            place,
            Electric(r,resDrawSimple,byGame{Grid<VoinSimple>()}),
            Enforcer(r,resDrawSimple,byGame{Grid<VoinSimple>()}),
            Staziser(r,resDrawSimple,byGame{Grid<VoinSimple>()}),
            Catapult(r,byGame{Grid<Catapult.obj>()}),
            Mine(r,byGame{Grid<PointControl>()}),
            Hospital(r,byGame{Grid<PointControl>()}),
            Stazis(r,byGame { Grid<Int>() })
    )

    val stager = Stager(byGame{Score()})

    val drawer = Drawer(pgser,exts.filterIsInstance<OnDraw>())
    val aimer = Aimer(exts.filterIsInstance<OnStopAim>())
    val maker = Maker(exts.filterIsInstance<OnMake>(),exts.filterIsInstance<OnMakeAfter>(),exts.filterIsInstance<OnMakeBefore>())
    val raiser = Raiser(
            pgser = pgser,
            stager = stager,
            exts = exts.filterIsInstance<OnRaise>(),
            aimer = aimer
    )

    val editor = Editor(exts)

    inner class CmderUnitcraft(mission:Int?) : CmderGame{
        val land = Land(mission,sizeFix)
        val pgser = land.pgser

        var game:Game by Delegates.notNull()


        init{
            reset()
        }

        override fun reset() {
            game = Game(
                    pgser = pgser,
                    canEdit = true,
                    drawer = drawer,
                    raiser = raiser,
                    editor = editor,
                    stager = stager,
                    maker = maker
            )
            gameCur = WeakReference(game)
            for((pg,v) in land.grid()) place.grid().set(pg,v)
            for((pg,v) in land.fixs()) place.fixs().set(pg,v)
        }

        override fun cmd(side: Side, cmd: String) {
            gameCur = WeakReference(game)
            game.cmd(side,cmd)
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

