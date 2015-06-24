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
            when (efk) {
                is EfkEnforce -> enforced[efk.voin] = true
            }
        }

        info(10) {
            when (msg) {
                is MsgRaise -> {
                    if (enforced[msg.src] == true) msg.isOn = true
                    if (rsVoin.voins.containsValue(msg.src)) for (pgNear in msg.pg.near) {
                        g.info(MsgVoin(pgNear)).voin?.let {
                            msg.add(pgNear, tlsAkt, EfkEnforce(pgNear, it))
                        }
                    }
                }
                is MsgDrawVoin -> enforced[msg.voin]?.let {
                    msg.drawTile(msg.pg, tlsEnforced(it))
                }
            }
        }

        stop(0) {
            when {
                efk is EfkEnforce && enforced[efk.voin!!] != null -> efk.stop()
            }
        }

        endTurn(0) {
            enforced.clear()
        }
    }
}

class EfkEnforce(val pg: Pg, var voin: Voin) : Efk()