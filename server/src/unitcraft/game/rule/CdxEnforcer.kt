package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.game.Game

class CdxEnforcer(r:Resource) : Cdx(r){
    val name = "enforcer"
    val extVoin = ExtVoin(r,name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land,g: Game) = rules{
        val rsVoin = extVoin.createRules(this,land,g)

        spot(0) {
            val voin = rsVoin[pgRaise]
            if(voin!=null) {
                val msg = MsgRaiseVoin(pgRaise,voin)
                if(g.trap(msg)) {
                    val r = raise(msg.isOn)
                    for (pgNear in pgRaise.near) {
                        r.add(pgNear, tlsAkt, EfkSttAdd(SttEnforcer(true), pgNear))
                    }
                }
            }
        }

        stop(0){
            if(msg is EfkSttAdd && msg.stt is SttEnforcer && msg.stter is HasStts){

            }

        }

        endTurn(0){
//            g.sttEach<SttEnforcer>{
//
//            }
//
//            g.sttExclude<SttEnforcer>{voin.side == g.sideTurn}
        }
    }
}

class SttEnforcer(val isActive:Boolean) : Stt()