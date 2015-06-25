package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.Collections
import java.util.WeakHashMap

class CdxVoid(r: Resource) : Cdx(r) {
    val name = "void"
    val extVoin = ExtVoin(r, name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land, g: Game) = rules {
        val rsVoin = extVoin.createRules(this, land, g)
        val hide: MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd, Boolean>())

        info<MsgRaise>(0) {
            if(rsVoin.voins.containsValue(src)) for (pgNear in pgRaise.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    add(pgNear, tlsAkt, EfkDmg(pgNear, it))
                }
            }
        }

        make<EfkUnhide>(0) {
            hide.remove(rsVoin[pg])
        }

        info<InfoIsHide>(0){
            if(voin in hide) hide()
        }

        endTurn(10) {
            for ((pg, v) in rsVoin.voins) {
                val side = v.side
                if (side != null) if (!g.stop(EfkHide(pg, side, v))) hide.add(v)
            }
        }
    }
}