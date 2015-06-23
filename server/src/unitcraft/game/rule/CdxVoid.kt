package unitcraft.game.rule

import unitcraft.game.Cdx
import unitcraft.game.Game
import unitcraft.game.Resource
import unitcraft.game.rules
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

        spot(0) {
            val r = rsVoin[pgRaise]?.let { raise(MsgRaiseVoin(pgRaise, it)) }
            if (r != null) for (pgNear in pgRaise.near)
                g.info(MsgVoin(pgNear)).voin?.let {
                    r.add(pgNear, tlsAkt, EfkDmg(pgNear, it))
                }
        }

        make(0) {
            if (msg is MsgUnhide) hide.remove(rsVoin[msg.pg])
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