package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import java.util.*
import kotlin.properties.Delegates

class Mover(r:Resource,val hider: Hider,val editor: Editor,val objs:()-> Objs) {
    val slotStopMove = ArrayList<(Move)->Boolean>()
    val slotMoveAfter = ArrayList<(Shape,Move)->Unit>()

    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (()->Boolean)? {
        if(slotStopMove.any{it(move)}) return null
        val objs = objClashed(move.shapeTo)
        if(objs.isEmpty()) return {true}
        val objHided = objs.filter{hider.isHided(it,move.sideVid)}
        if(objHided.isEmpty()) return null
        return { hider.reveal(objHided);false}
    }

    fun move(move: Move) {
        if(objClashed(move.shapeTo).isNotEmpty()) throw Err("cant move obj=${move.obj} to shape=${move.shapeTo}")
        val shapeFrom = move.obj.shape
        move.obj.shape = move.shapeTo
        slotMoveAfter.forEach{it(shapeFrom,move)}
    }

    private fun objClashed(shape: Shape):List<Obj>{
        return objs().filter{obj -> shape.pgs.any{it in obj.shape.pgs}}
    }

    fun canCreate(shape:Shape):Boolean{
        return objClashed(shape).isEmpty()
    }

    fun create(shape:Shape):Obj{
        val obj = Obj(shape)
        //creates.forEach{ it(obj) }
        if(objClashed(shape).isNotEmpty()) throw Err("cant create obj with shape=$shape")
        objs().add(obj)
        return obj
    }
}

class Move(
        val obj: Obj,
        val shapeTo: Shape,
        val sideVid: Side
)