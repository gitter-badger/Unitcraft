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
        val enforced = WeakHashMap<Any, Boolean>()

        spot(0) {
            val voin = rsVoin[pgRaise]
            if (voin != null) {
                val msg = MsgRaiseVoin(pgRaise, voin)
                if (g.trap(msg)) {
                    val r = raise(msg.isOn)
                    for (pgNear in pgRaise.near) {
                        r.add(pgNear, tlsAkt, EfkEnforce(pgNear))
                    }
                }
            }
        }

        make(0) {
            when (msg) {
                is EfkEnforce -> {
                    val aim = msg.aim!!
                    enforced[aim] = true
                }
                is MsgDraw -> {
                    val enf = enforced[msg.what]
                    if (enf != null) msg.draw {
                        drawTile(msg.pg, tlsEnforced(enf))
                    }
                }
            }
        }

        trap(10){
            if(msg is MsgRaiseVoin && enforced[msg.voin]==true) msg.isOn = true
        }

        stop(0) {
            when {
                msg is EfkEnforce && enforced[msg.aim!!] != null -> msg.stop()

            }
        }

        endTurn(0) {
            enforced.clear()
        }
    }
}

class EfkEnforce(val pg: Pg, var aim: Any? = null) : Efk() {
    override fun isOk() = aim != null
}