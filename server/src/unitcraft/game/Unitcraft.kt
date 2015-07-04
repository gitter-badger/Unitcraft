package unitcraft.game

import unitcraft.land.Land
import unitcraft.server.CreatorGame
import unitcraft.server.IGame
import unitcraft.game.Game
import unitcraft.game.rule.*

class Unitcraft(r:Resource = Resource()) : CreatorGame {
    val resDrawer = ResDrawer(r)
    val breeds = listOf(BreedElectric(r),BreedEnforcer(r))
    val tpPiles = listOf(TpCatapult(r))
    val tpPilePointControls = listOf(TpMine(r),TpHospital(r))

    override fun createGame(mission:Int?):()-> IGame {
        val land = Land(mission)
        val pgser = land.pgser

        return {
            val herds = breeds.map{Herd(it)}
            val piles = tpPiles.map{Pile(it)}
            val pilePointControls = tpPilePointControls.map{PilePointControl(it)}
            val place = Place(land)
            val stazis = Stazis()
            val drawer = Drawer(
                    r = resDrawer,
                    pgser = pgser,
                    place = place,
                    herds = herds,
                    piles = piles,
                    pilePointControls = pilePointControls,
                    stazis = stazis
            )
            Game(
                    pgser = pgser,
                    canEdit=true,
                    stazis = stazis,
                    place = place,
                herds = herds,
                    drawer = drawer
        ) }
    }
}