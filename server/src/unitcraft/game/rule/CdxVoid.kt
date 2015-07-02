package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class CdxVoid(r: Resource) : CdxVoin(r) {
    val name = "void"
    val tlsVoin = r.tlsVoin(name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()

        ruleVoin(g,voins,resVoin,tlsVoin)

        aimByHand(g,voins,resVoin){ pg,pgRaise,voinRaise,sideVid,r ->
            g.voin(pg,sideVid)?.let {
                r.add(pg, tlsAkt, EfkDmg(pg, it))
            }
        }

        val hide : MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd,Boolean>())

        make<EfkUnhide>(0) {
            voins[pg]?.let{hide.remove(it)}
        }

        info<MsgIsHided>(0){
            if(voin in hide) yes()
        }

        endTurn(10) {
            for ((pg, v) in voins) {
                val side = v.side
                if (side != null) if (!g.stop(EfkHide(pg, side, v))) hide.add(v)
            }
        }
    }
}