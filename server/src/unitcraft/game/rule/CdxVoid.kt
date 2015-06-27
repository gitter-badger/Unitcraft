package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class CdxVoid(r: Resource) : Cdx(r) {
    val name = "void"
    val extVoin = ExtVoin(r, name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land, g: Game) = rules {
        val voins = Grid<VoinVoid>()
        extVoin.createRules(this, g,ExtVoin.fromGrid(voins,::VoinVoid))

        info<MsgRaise>(0) {
            if(voins.has(src)) for (pgNear in pgRaise.near) {
                g.info(MsgVoin(pgNear)).voin?.let {
                    add(pgNear, tlsAkt, EfkDmg(pgNear, it))
                }
            }
        }

        make<EfkUnhide>(0) {
            voins[pg]?.isHided = true
        }

        info<MsgIsHided>(0){
            if(voin is VoinVoid && voin.isHided) yes()
        }

        endTurn(10) {
            for ((pg, v) in voins) {
                val side = v.side
                if (side != null) if (!g.stop(EfkHide(pg, side, v))) v.isHided = true
            }
        }
    }
}

class VoinVoid(side: Side?, life: Int, flip: Boolean) : ExtVoin.VoinStd(side,life,flip){
    var isHided = false
}