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

        make<EfkEnforce>(0) {
            enforced[voin] = true
        }

        info<MsgRaise>(10) {
            if (enforced[src] == true) isOn = true
            if (rsVoin.voins.containsValue(src)) for (pgNear in pgRaise.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    add(pgNear, tlsAkt, EfkEnforce(pgNear, it))
                }
            }
        }

        info<MsgDrawVoin>(10) {
            enforced[voin]?.let { drawTile(pg, tlsEnforced(it)) }
        }

        stop<EfkEnforce>(0) {
            if(enforced[voin] != null) stop()
        }

        endTurn(0) {
            enforced.clear()
        }
    }
}

class EfkEnforce(val pg: Pg, var voin: Voin) : Efk()