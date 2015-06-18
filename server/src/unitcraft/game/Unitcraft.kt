package unitcraft.game

import unitcraft.game.rule.CdxCatapult
import unitcraft.game.rule.CdxEnforcer
import unitcraft.game.rule.CdxPlace
import unitcraft.game.rule.CdxStaziser
import unitcraft.land.Land
import unitcraft.server.CreatorGame
import unitcraft.server.IGame
import unitcraft.game.Game

class Unitcraft() : CreatorGame {
    val cdxs = Resource().createRules(kCdxs)

    override fun createGame(mission:Int?):()-> IGame {
        val land = Land(mission, cdxs)
        return { Game(cdxs, land, true) }
    }

    companion object{
        val kCdxs = listOf(
                CdxEnforcer::class,
                CdxPlace::class,
                CdxCatapult::class,
                CdxStaziser::class
        )
    }
}