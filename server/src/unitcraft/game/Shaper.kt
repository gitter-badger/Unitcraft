package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Shaper(r:Resource,val hider: Hider,val objs:()-> Objs) {

    val stops = ArrayList<(Move)->Boolean>()
//    val stopAim = exts.filterIsInstance<OnStopAim>()
//    val arm = exts.filterIsInstance<OnArm>()
//    val getBusys = exts.filterIsInstance<OnGetBusy>()
    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (()->Boolean)? {
        val obj = objClashed(move.shapeTo) ?: return {true}
        return hider.isHided(obj,move.sideVid)?.let{ {it();false} }
    }

    private fun objClashed(shape: Shape):Obj?{
        val sameZetOrd = objs().byZetOrder(shape.zetOrder)
        // TODO quadr may clash with 2 objs
        return sameZetOrd.firstOrNull{obj -> shape.pgs.any{it in obj.shape.pgs}}
    }

    fun create(kind:Kind,shape:Shape):Obj{
        if(objClashed(shape)!=null) throw Err("cant create obj(shape=$shape kind=$kind")
        val obj = Obj(kind,shape)
        objs().add(obj)
        return obj
    }

    fun remove(obj:Obj):Boolean{
        return objs().remove(obj)
    }
}

class Move(
        val obj: Obj,
        val shapeTo: Shape,
        val sideVid: Side
)

enum class ZetOrder {
    flat, voin, fly
}

class SkilMove(r:Resource,val shaper: Shaper){
    val tlsMove = r.tlsAktMove
    fun pgs(pgSpot:Pg,obj:Obj,sideVid:Side,r:Raise){
        for(pg in pgSpot.near) {
            val shapeTo = obj.shape.copy(head=pg)
            val can = shaper.canMove(Move(obj, shapeTo, sideVid))
            if (can!=null)
                r.add(pg, tlsMove) {
                    if(can()) obj.shape = shapeTo
                }
        }
    }
}