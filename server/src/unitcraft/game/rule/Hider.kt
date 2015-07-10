package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.server.Side
import java.util.*

class Hider {
//    val hides = ArrayList<(Obj)->Boolean>()
//    val isVidPgs = ArrayList<(Pg)->Boolean>()

    fun isHided(obj:Obj,sideVid: Side):(()->Unit)?{
         if(obj.kind.name == "KindInviser") return {() -> println("reveal")} else return null
    }

    fun hide(obj: Obj,src:Any) {

    }
//
//    fun unhide(obj: Obj,src:Any) {
//
//    }
//
//    fun

    //    fun getBusy(pg: Pg, zetOrder: ZetOrder, side: Side): Busy? {
    //        if(zetOrder!=ZetOrder.unit)  return null
    //        return grid()[pg]?.let{ if(it.isHided) Busy{ it.isHided = false } else Busy() }
    //    }
}

