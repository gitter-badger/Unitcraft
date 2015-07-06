package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class Inviser(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd{
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
//    endTurn(10) {
//        for ((pg, v) in voins) {
//            val side = v.side
//            if (side != null) if (!g.stop(EfkHide(pg, side, v))) hide.add(v)
//        }
//    }
}