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

        info(0) {
            if(msg is MsgRaise) if(rsVoin.voins.containsValue(msg.src)) for (pgNear in msg.pg.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    msg.add(pgNear, tlsAkt, EfkDmg(pgNear, it))
                }
            }
        }

        make(0) {
            if (efk is EfkUnhide) hide.remove(rsVoin[efk.pg])
        }

        info(0){
            if(msg is InfoIsHide) if(msg.voin in hide) msg.hide = true
        }

        endTurn(10) {
            for ((pg, v) in rsVoin.voins) {
                val side = v.side
                if (side != null) if (!g.stop(EfkHide(pg, side, v))) hide.add(v)
            }
        }
    }
}