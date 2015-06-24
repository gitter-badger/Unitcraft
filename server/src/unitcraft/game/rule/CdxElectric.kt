package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.Collections
import java.util.WeakHashMap

class CdxElectric(r: Resource) : Cdx(r) {
    val name = "electric"
    val extVoin = ExtVoin(r, name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land, g: Game) = rules {
        val rsVoin = extVoin.createRules(this, land, g)
        val hide: MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd, Boolean>())

        info(0) {
            if(msg is MsgRaise) if(rsVoin.voins.containsValue(msg.src)) for (pgNear in msg.pg.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    msg.add(pgNear, tlsAkt, EfkDmg(pgNear, it))
                }
            }
        }
    }
}