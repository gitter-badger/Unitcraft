package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class CdxRedeployer(r: Resource) : CdxVoin(r) {
    val name = "redeployer"
    val tlsVoin = r.tlsVoin(name)
    val tlsAkt = r.tlsAkt(name)
    val tlsBuild = r.tlsAkt(name,"build")

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()

        ruleVoin(g,voins,resVoin,tlsVoin)

        aimByHand(g,voins,resVoin){ pg,pgRaise,voinRaise,sideVid,r ->
            g.voin(pg,sideVid)?.let {
                if(it.side!=null && it.life>=3) r.add(pg, tlsAkt, EfkSell(pg, it))
            }
            if(!g.stop(EfkBuild(pg))) r.add(pg, tlsBuild, EfkBuild(pg))
        }

        make<EfkSell>(0) {
//            g.make(EfkRemove(pg, voinAim))
//            g.make(EfkGold(5, voinAim.side!!))
        }

        make<EfkGold>(0){
            for((pg,voin) in voins) g.make(EfkHeal(pg,voin, gold))
        }
    }
}

class EfkSell(val pg:Pg,val voinAim:Voin) : Efk()

class EfkGold(val gold:Int,val side: Side) : Efk()
