package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*
import kotlin.properties.Delegates

class Mover(r:Resource,val stager: Stager,val objs:()-> Objs) {
    val slotStopMove = ArrayList<(Move)->Boolean>()
    val slotStopBuild = ArrayList<(Shape)->Boolean>()
    val slotMoveAfter = ArrayList<(Shape,Move)->Unit>()
    val slotHide = ArrayList<(Obj)->Boolean>()

    init{
        stager.onStartTurn {
            objs().forEach { obj ->
               obj.hide = slotHide.any{it(obj)}
            }
        }
    }

    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (()->Boolean)? {
        if(slotStopMove.any{it(move)}) return null
        return canBusy(move.shapeTo,move.sideVid)
    }

    fun canBulid(shape: Shape,sideVid:Side): (()->Boolean)? {
        if(slotStopBuild.any{it(shape)}) return null
        return canBusy(shape,sideVid)
    }

    private fun canBusy(shape:Shape,sideVid:Side):(()->Boolean)?{
        val objs = objs().byClash(shape)
        if(objs.isEmpty()) return {true}
        val objHided = objs.filter{it.hide && it.side==sideVid.vs}
        if(objHided.isEmpty()) return null
        return { reveal(objHided);false}
    }

    fun move(move: Move) {
        if(objs().byClash(move.shapeTo).isNotEmpty()) throw Err("cant move obj=${move.obj} to shape=${move.shapeTo}")
        val shapeFrom = move.obj.shape
        move.obj.shape = move.shapeTo
        for(side in Side.ab) {
            val pgsWt = pgsWatch(side)
            objs().filter { it.shape.pgs.intersect(pgsWt).isNotEmpty() }.forEach { it.hide = false }
        }
        slotMoveAfter.forEach{it(shapeFrom,move)}
    }

    private fun pgsWatch(side:Side) = objs().bySide(side).flatMap { it.near() }.distinct()

    fun rehide(){
        objs().forEach { obj ->
            if (!slotHide.any { it(obj) }) obj.hide = false
        }
    }

    fun reveal(objs: List<Obj>) {
        objs.forEach { it.hide = false }
    }

    class CanHide(var can:Boolean) : Data
}

class Move(
        val obj: Obj,
        val shapeTo: Shape,
        val sideVid: Side
)