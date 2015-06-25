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

        info<MsgRaise>(0) {
             if(rsVoin.voins.containsValue(src)) for (pgNear in pgRaise.near)
                g.info(MsgVoin(pgNear)).voin?.let {
                    add(pgNear, tlsAkt, EfkDmg(pgNear, it))
                }
        }
    }
}