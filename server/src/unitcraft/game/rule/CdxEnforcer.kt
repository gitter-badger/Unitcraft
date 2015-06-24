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
            if(msg is MsgRaise) {
                if(enforced[msg.voin]==true) msg.isOn = true
                if(rsVoin.voins.containsValue(msg.voin)) for (pgNear in msg.pg.near) {
                    g.info(MsgVoin(pgNear)).voin?.let {
                        msg.add(pgNear, tlsAkt, EfkEnforce(pgNear, it))
                    }
                }
            }
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