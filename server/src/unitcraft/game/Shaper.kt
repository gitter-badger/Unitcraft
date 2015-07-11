package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Shaper(r:Resource,val hider: Hider,val objs:()-> Objs) {

    val stopMoves = ArrayList<(Move)->Boolean>()

    //val creates = ArrayList<(Obj)->Unit>()
//    val stopAim = exts.filterIsInstance<OnStopAim>()
//    val arm = exts.filterIsInstance<OnArm>()
//    val getBusys = exts.filterIsInstance<OnGetBusy>()
    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (()->Boolean)? {
        if(stopMoves.any{it(move)}) return null
        val obj = objClashed(move.shapeTo) ?: return {true}
        return hider.isHided(obj,move.sideVid)?.let{ {it();false} }
    }

    fun move(move: Move) {
        if(objClashed(move.shapeTo)!=null) throw Err("cant move obj=${move.obj} to shape=${move.shapeTo}")
        move.obj.shape = move.shapeTo
    }

    private fun objClashed(shape: Shape):Obj?{
        val sameZetOrd = objs().byZetOrder(shape.zetOrder)
        // TODO quadr may clash with 2 objs
        return sameZetOrd.firstOrNull{obj -> shape.pgs.any{it in obj.shape.pgs}}
    }

    fun create(kind:Kind,shape:Shape):Obj{
        val obj = Obj(kind,shape)
        //creates.forEach{ it(obj) }
        if(objClashed(shape)!=null) throw Err("cant create obj with shape=$shape kind=$kind")
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