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
        val voins = ExtVoin.std()
        extVoin.createRules(this,g,voins)
        val enforced = WeakHashMap<Voin, Boolean>()

        make<EfkEnforce>(0) {
            enforced[voin] = true
        }

        info<MsgTgglRaise>(0) {
            if (enforced[src]?:false) isOn = true
        }

        info<MsgRaise>(10) {
            if (voins.has(src)) for (pgNear in pgRaise.near) {
                g.voin(pgNear,sideVid)?.let {
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