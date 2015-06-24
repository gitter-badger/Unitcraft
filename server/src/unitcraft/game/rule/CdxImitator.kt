package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.WeakHashMap

class CdxImitator(r: Resource) : Cdx(r) {
    val name = "imitator"
    val extVoin = ExtVoin(r, name)

    override fun createRules(land: Land, g: Game) = rules {
        val rsVoin = extVoin.createRules(this, land, g)

        info(0) {
            if(msg is MsgSpot) {
                val voin = rsVoin[msg.pg]
                if (voin != null) {
                    for (pgNear in msg.pg.near) {
                        g.info(MsgVoin(pgNear)).voins.forEach {
                            val r = g.info(MsgRaise(pgNear, it)).raise
                            msg.add(r)
                        }
                    }
                }
            }
        }
    }
}