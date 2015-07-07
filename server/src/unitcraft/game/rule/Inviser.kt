package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class Inviser(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd,OnEndTurn,OnGetBusy{
    override val tlsVoin = r.tlsVoin("inviser")
//    val hide : MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd,Boolean>())

//    make<EfkUnhide>(0) {
//        //voins[pg]?.let{hide.remove(it)}
//    }
//
//    info<MsgIsHided>(0){
//        if(voin in hide) yes()
//    }
//
    override fun getBusy(pg: Pg, tpMove: TpMove, side: Side): Busy? {
        if(tpMove!=TpMove.unit)  return null
        return grid()[pg]?.let{ if(it.isHided) BusyInvis{ it.isHided = false } else BusyStd }
    }

    override fun onEndTurn(side:Side) {
        for ((pg, v) in grid()) {
            if (v.side == side.vs) v.isHided = true
        }
    }
}