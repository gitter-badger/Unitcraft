package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.WeakHashMap

class CdxImitator(r: Resource) : CdxVoin(r) {
    val name = "imitator"
    val tlsVoin = r.tlsVoin(name)

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()
        ruleVoin(g,voins,resVoin,tlsVoin)

        ruleTgglRaiseBySideTurn(g,voins)

        info<MsgSpot>(0) {
            voins[pgSrc]?.let {imitator ->
                if(pgSpot==pgSrc) for (pgNear in pgSpot.near) {
                    g.voins(pgNear, side).forEach {
                        val spot = g.info(MsgSpot(g,pgSpot,side,pgNear))
                        raises.addAll(spot.raises)
                    }
                }
            }
        }
    }
}