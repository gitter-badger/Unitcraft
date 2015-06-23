package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game
import java.util.*

class CdxVoid(r:Resource): Cdx(r){
    val name = "void"
    val extVoin = ExtVoin(r,name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land,g: Game) = rules{
        val rsVoin = extVoin.createRules(this,land,g)
        val hide : MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd,Boolean>())

        spot(0){
            rsVoin[pgRaise]?.let {
                val r = raise(MsgRaiseVoin(pgRaise, it))
                if (r != null) for (pgNear in pgRaise.near) {
                    r.add(pgNear, tlsAkt, EfkDmg(pgNear))
                }
            }
        }

        make(0){
            if(msg is MsgUnhide) hide.remove(rsVoin[msg.pg])
        }

        endTurn(10) {
            for((pg,v) in rsVoin.voins) {
                val side = v.side
                if(side!=null) {
                    val efk = EfkHide(pg, side, v)
                    if(g.stop(efk)) hide.add(v)
                }
            }
        }
    }
}