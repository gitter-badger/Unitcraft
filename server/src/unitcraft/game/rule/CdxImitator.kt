package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.WeakHashMap

class CdxImitator(r: Resource) : Cdx(r) {
    val name = "imitator"
    val extVoin = ExtVoin(r, name)

    override fun createRules(land: Land, g: Game) = rules {
        val voins = ExtVoin.std()
        extVoin.createRules(this, g, voins)

        info<MsgSpot>(0) {
            val imitator = voins[pgSpot]
            if (imitator != null) {
                for (pgNear in pgSpot.near) {
                    g.info(MsgVoin(pgNear)).all.forEach {
                        add(g.info(MsgRaise(g,pgSpot,it,imitator)))
                    }
                }
            }
        }
    }
}