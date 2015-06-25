package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.WeakHashMap

class CdxImitator(r: Resource) : Cdx(r) {
    val name = "imitator"
    val extVoin = ExtVoin(r, name)

    override fun createRules(land: Land, g: Game) = rules {
        val rsVoin = extVoin.createRules(this, land, g)

        info<MsgSpot>(0) {
            val voin = rsVoin[pgSpot]
            if (voin != null) {
                for (pgNear in pgSpot.near) {
                    g.info(MsgVoin(pgNear)).all.forEach {
                        add(g.info(MsgRaise(g,pgSpot,it,voin)))
                    }
                }
            }
        }
    }
}