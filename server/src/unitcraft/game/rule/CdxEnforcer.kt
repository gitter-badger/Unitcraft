package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.game.Game

class CdxEnforcer(r:Resource) : Cdx(r){
    val name = "enforcer"
    val extVoin = ExtVoin(r,name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land,g: Game) = rules{
        val gridVoin = extVoin.createRules(this,land,g)

        spot(0) {
            val voin = gridVoin[pgRaise]
            if(voin!=null) {
                val r = raise(voin.side)
                for (pgNear in pgRaise.near) if (g.can(From(pgRaise).voin(voin.side), Aim(pgNear), TpMake.skil)) {
                    r.akt(pgNear, tlsAkt) { g.sttAdd(pgNear,SttEnforcer(true)) }
                }
            }
        }

        endTurn(0){
            g.sttEach<SttEnforcer>{

            }

            g.sttExclude<SttEnforcer>{voin.side == g.sideTurn}
        }
    }
}

class Stt

class SttEnforcer(val isActive:Boolean)