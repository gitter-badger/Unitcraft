package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game

class CdxVoid(r:Resource): Cdx(r){
    val name = "void"
    val extVoin = ExtVoin(r,name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land,g: Game) = rules{
        val rsVoin = extVoin.createRules(this,land,g)

        spot(0){
            val voin = rsVoin[pgRaise]
            if(voin!=null) {
                val msg = MsgRaiseVoin(pgRaise,voin)
                if(g.trap(msg)) {
                    val r = raise(msg.isOn)
                    for (pgNear in pgRaise.near) {
                        r.add(pgNear, tlsAkt, EfkDmg(pgNear))
                    }
                }
            }
        }

        endTurn(10) {
            for((pg,v) in rsVoin.voins) {
                val side = v.side
                if(side!=null) {
                    val efk = EfkHide(pg, side, v)
                    if (g.trap(efk)) g.make(efk)
                }
            }
        }
    }
}