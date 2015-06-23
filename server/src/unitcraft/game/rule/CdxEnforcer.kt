package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.WeakHashMap

class CdxEnforcer(r: Resource) : Cdx(r) {
    val name = "enforcer"
    val extVoin = ExtVoin(r, name)
    val tlsAkt = r.tlsAkt(name)
    val tlsEnforced = r.tlsBool("enforced", "enforcedAlready")

    override fun createRules(land: Land, g: Game) = rules {
        val rsVoin = extVoin.createRules(this, land, g)
        val enforced = WeakHashMap<Voin, Boolean>()

        spot(0) {
            val voin = rsVoin[pgRaise]
            if (voin != null) {
                val r = raise(MsgRaiseVoin(pgRaise, voin))
                if(r!=null) for (pgNear in pgRaise.near) {
                    g.info(MsgVoin(pgNear)).voin?.let {
                        r.add(pgNear, tlsAkt, EfkEnforce(pgNear,it))
                    }
                }
            }
        }

        make(0) {
            when (msg) {
                is EfkEnforce -> enforced[msg.voin] = true
                is MsgDrawVoin -> {
                    val enf = enforced[msg.voin]
                    if (enf != null) msg.draw {
                        drawTile(msg.pg, tlsEnforced(enf))
                    }
                }
            }
        }

        info(10){
            if(msg is MsgRaiseVoin && enforced[msg.voin]==true) msg.isOn = true
        }

        stop(0) {
            when {
                msg is EfkEnforce && enforced[msg.voin!!] != null -> msg.stop()

            }
        }

        endTurn(0) {
            enforced.clear()
        }
    }
}

class EfkEnforce(val pg: Pg, var voin: Voin) : Efk()