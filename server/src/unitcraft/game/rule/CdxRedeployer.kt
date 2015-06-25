package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class CdxRedeployer(r: Resource) : Cdx(r) {
    val name = "redeployer"
    val extVoin = ExtVoin(r, name)
    val tlsAkt = r.tlsAkt(name)
    val tlsBuild = r.tlsAkt(name,"build")

    override fun createRules(land: Land, g: Game) = rules {
        val rsVoin = extVoin.createRules(this, land, g)

        info<MsgRaise>(0) {
            if(rsVoin.voins.containsValue(src)) for (pgNear in pgRaise.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    if(it.side!=null && it.life>=3) add(pgNear, tlsAkt, EfkSell(pgNear, it))
                }
                add(pgNear, tlsBuild, EfkBuild(pgNear))
            }
        }

        make<EfkSell>(0) {
            g.make(EfkRemove(pg, voinAim))
            g.make(EfkGold(5, voinAim.side!!))
        }

        make<EfkGold>(0){
            rsVoin.voins.forEach { pg, voinStd -> g.make(EfkHeal(pg,voinStd, gold)) }
        }
    }
}

class EfkSell(val pg:Pg,val voinAim:Voin) : Efk()

class EfkGold(val gold:Int,val side: Side) : Efk()
