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

    val place = Place(byGame{Grid<TpPlace>()},byGame{Grid<Map<TpPlace,Int>>()})
    val breeds = listOf(
            BreedElectric(r,byGame{Grid<VoinSimple>()}),
            BreedEnforcer(r,byGame{Grid<VoinSimple>()})
    )
    val tpPiles = listOf(TpCatapult(r,byGame{Grid<TpPile.obj>()}))
    val tpPointControls = listOf(
            TpMine(r,byGame{Grid<PointControl>()}),
            TpHospital(r,byGame{Grid<PointControl>()})
    )
    val stazis = Stazis(byGame{Grid<Int>()})

    val resDrawer = ResDrawer(r)
    val drawer = Drawer(
            r = resDrawer,
            pgser = {gameCur.get()!!.pgser},
            place = place,
            breeds = breeds,
            tpPiles = tpPiles,
            tpPointControls = tpPointControls,
            stazis = stazis
    )

    inner class CmderUnitcraft(mission:Int?) : CmderGame{
        val land = Land(mission)
        val pgser = land.pgser

        var game:Game by Delegates.notNull()


        init{
            reset()
        }

        override fun reset() {
            game = Game(
                    pgser = pgser,
                    canEdit = true,
                    drawer = drawer
            )
            gameCur = WeakReference(game)
            for((pg,v) in land.grid()) drawer.place.grid().set(pg,v)
            for((pg,v) in land.fixs()) drawer.place.fixs().set(pg,v)
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

