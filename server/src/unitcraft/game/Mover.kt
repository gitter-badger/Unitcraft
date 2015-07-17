package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import java.util.*
import kotlin.properties.Delegates

class Mover(r:Resource,val editor: Editor,val objs:()-> Objs) {
    val slotStopMove = ArrayList<(Move)->Boolean>()
    val slotStopBuild = ArrayList<(Shape)->Boolean>()
    val slotMoveAfter = ArrayList<(Shape,Move)->Unit>()

    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (()->Boolean)? {
        if(slotStopMove.any{it(move)}) return null
        val objs = objs().byClash(move.shapeTo)
        if(objs.isEmpty()) return {true}
        val objHided = objs.filter{isHided(it,move.sideVid)}
        if(objHided.isEmpty()) return null
        return { reveal(objHided);false}
    }

    fun canBulid(shape: Shape,sideVid:Side): (()->Boolean)? {
        if(slotStopBuild.any{it(shape)}) return null
        val objs = objs().byClash(shape)
        if(objs.isEmpty()) return {true}
        val objHided = objs.filter{isHided(it,sideVid)}
        if(objHided.isEmpty()) return null
        return { reveal(objHided);false}
    }

    fun move(move: Move) {
        if(objs().byClash(move.shapeTo).isNotEmpty()) throw Err("cant move obj=${move.obj} to shape=${move.shapeTo}")
        val shapeFrom = move.obj.shape
        move.obj.shape = move.shapeTo
        slotMoveAfter.forEach{it(shapeFrom,move)}
    }

    fun isHided(obj:Obj,sideVid: Side):Boolean{
        return false
    }

    fun reveal(objs: List<Obj>) {
        println("reveal: "+objs)
    }
}

class Move(
        val obj: Obj,
        val shapeTo: Shape,
        val sideVid: Side
)

//TODO мовер и хидер одно целое