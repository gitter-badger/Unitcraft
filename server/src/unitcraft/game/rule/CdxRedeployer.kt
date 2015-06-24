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

        info(0) {
            if(msg is MsgRaise) if(rsVoin.voins.containsValue(msg.src)) for (pgNear in msg.pg.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    if(it.side!=null && it.life>=3) msg.add(pgNear, tlsAkt, EfkSell(pgNear, it))
                }
                msg.add(pgNear, tlsBuild, EfkBuild(pgNear))
            }
        }

        make(0) {
            when (efk) {
                is EfkSell -> {
                    g.make(EfkRemove(efk.pg, efk.voin))
                    g.make(EfkGold(5, efk.voin.side!!))
                }
                is EfkGold -> rsVoin.voins.forEach { pg, voinStd -> g.make(EfkHeal(pg,voinStd, efk.gold)) }
            }
        }
    }
}

class EfkSell(val pg:Pg,val voin:Voin) : Efk()

class EfkGold(val gold:Int,val side: Side) : Efk()
