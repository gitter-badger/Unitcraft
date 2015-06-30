package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import java.util.WeakHashMap

class CdxEnforcer(r: Resource) : CdxVoin(r) {
    val name = "enforcer"
    val tlsVoin = r.tlsVoin(name)
    val tlsAkt = r.tlsAkt(name)
    val tlsEnforced = r.tlsBool("enforced", "enforcedAlready")

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()
        val enforced = WeakHashMap<Voin, Boolean>()

        ruleVoin(g,voins,resVoin,tlsVoin)

        skilByHandWithMove(g,voins,resVoin){ pg,pgRaise,voinRaise,sideVid,r ->
            g.voin(pg,sideVid)?.let {
                if(enforced[it] == null) r.add(pg, tlsAkt, EfkEnforce(pg, it))
            }
        }

        make<EfkEnforce>(0) {
            enforced[voin] = true
        }

        info<MsgTgglRaise>(0) {
            if (enforced[voinRaise]?:false) isOn = true
        }

//        info<MsgRaise>(10) {
//            if (voins.has(src)) for (pgNear in pgRaise.near) {
//                g.voin(pgNear,sideVid)?.let {
//                    add(pgNear, tlsAkt, EfkEnforce(pgNear, it))
//                }
//            }
//        }

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