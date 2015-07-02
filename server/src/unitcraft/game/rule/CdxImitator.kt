package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land

class CdxImitator(r: Resource) : CdxVoin(r) {
    val name = "imitator"
    val tlsVoin = r.tlsVoin(name)

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()
        ruleVoin(g, voins, resVoin, tlsVoin)

        ruleTgglRaiseBySideTurn(g, voins)

        info<MsgSpot>(0) {
            if (pgSpot != pgSrc) return@info
            voins[pgSrc]?.let { imitator ->
                for (pgNear in pgSpot.near) {
                    if (g.voins(pgNear, side).isNotEmpty()) {
                        val spot = g.info(MsgSpot(g, pgSpot, side, pgNear))
                        raises.addAll(spot.raises)
                    }
                }
                if (sloys().isEmpty()) {
                    val tggl = g.info(MsgTgglRaise(pgSpot, imitator))
                    val r = Raise(pgSpot,tggl.isOn)
                    for (pgNear in pgSpot.near) {
                        val efk = EfkMove(pgSpot, pgNear, imitator)
                        if (!g.stop(efk)) r.add(pgNear, resVoin.tlsMove, efk)
                    }
                }
            }
        }
    }
}