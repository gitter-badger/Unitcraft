package unitcraft.game

import unitcraft.game.rule.Obj
import unitcraft.game.rule.Objs
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Mover(r:Resource) {
    val stager: Stager by inject()
    val tracer: Tracer by inject()

    val objs: () -> Objs by injectObjs()
    val slotStopMove = ArrayList<(Move) -> Boolean>()
    val slotStopAdd = ArrayList<(Pg, Side) -> Boolean>()
    val slotMoveAfter = ArrayList<(Move) -> Boolean>()
    val slotHide = ArrayList<(Obj) -> Boolean>()

    val tileReveal = r.tile("reveal")

    init {
        stager.onStartTurn { side ->
            objs().forEach { obj ->
                if (obj.side == side && slotHide.any { it(obj) }) obj.hide = true
            }
        }
    }

    /**
     * Перемещает объект, если это возможно.
     * null - движение недоступно
     * ()->Boolean - совершает движение и выполняет ifMove, или если невидимка в pgTo, то раскрывает его
     */
    fun canMove(obj: Obj, pgFrom: Pg, pgTo: Pg, sideVid: Side, isKick: Boolean = false, ifMove: () -> Unit): (() -> Boolean)? {
        val move = Move(obj, pgFrom, pgTo, sideVid, isKick)
        if (slotStopMove.any { it(move) }) return null
        val obj = objs()[pgTo] ?: return {
            val abort = move(move)
            ifMove()
            abort
        }
        return if (obj.isVid(sideVid)) null else {
            val fn = { reveal(obj);true };fn
        }
    }

    fun canMove(obj: Obj, pgTo: Pg, sideVid: Side, isKick: Boolean = false, ifMove: () -> Unit) =
            canMove(obj, obj.pg, pgTo, sideVid, isKick, ifMove)

    fun isMove(obj: Obj, pgFrom: Pg, pgTo: Pg, sideVid: Side, isKick: Boolean = false):Boolean{
        val move = Move(obj, pgFrom, pgTo, sideVid, isKick)
        return if (slotStopMove.any { it(move) }) false else {
            !(objs()[pgTo]?.isVid(sideVid)?:false)
        }
    }

    fun isMove(obj: Obj, pgTo: Pg, sideVid: Side, isKick: Boolean = false)=
        isMove(obj, obj.pg,pgTo, sideVid, isKick)

    /**
     * Добавляет объект на поле, если это возможно.
     * null - движение недоступно
     * ()->Unit - совершает движение и выполняет ifMove, или еслив pgTo невидимка, то раскрывает его
     */
    fun canAdd(pg: Pg, sideVid: Side, ifAdd: (Obj,Int) -> Unit): ((Int) -> Unit)? {
        if (slotStopAdd.any { it(pg, sideVid) }) return null
        val obj = objs()[pg] ?: return {num ->
            Obj(pg).apply {
                ifAdd(this,num)
                if (objs()[this.pg] != null) throw Err("obj=$this clash")
                objs().list.add(this)
                if (slotHide.any { it(this) }) this.hide = true
                watch()
            }
        }
        return if (obj.isVid(sideVid)) null else {
            val fn = {num:Int -> reveal(obj) };fn
        }
    }

    private fun move(move: Move): Boolean {
        if (move.obj.pg != move.pgFrom) throw Err("obj.pg(${move.obj.pg} != pgFrom(${move.pgFrom})")
        if (objs()[move.pgTo] != null) throw Err("cant move obj=${move.obj} to pg=${move.pgTo}")
        move.obj.pg = move.pgTo
        watch()
        return slotMoveAfter.map { it(move) }.any { it }
    }

    private fun watch(){
        for (side in Side.ab) {
            val pgsWt = pgsWatch(side)
            objs().filter { !it.isVid(side) && it.pg in pgsWt }.forEach { reveal(it) }
        }
    }

    private fun pgsWatch(side: Side) = objs().bySide(side).flatMap { it.near() }.distinct()

    fun revealUnhided() {
        objs().forEach { obj ->
            if (!slotHide.any { it(obj) }) reveal(obj)
        }
    }

    fun reveal(obj: Obj) {
        if(obj.hide) {
            obj.hide = false
            tracer.touch(obj.pg, tileReveal)
        }
    }
}

/**
 * Событие изменения позиции объекта.
 * если isKick, значит объект перемещается не сам
 */
class Move(val obj: Obj, val pgFrom: Pg, val pgTo: Pg, val sideVid: Side, val isKick: Boolean = false) {
    //constructor(obj: Obj, pgTo: Pg, sideVid: Side, isKick: Boolean = false) : this(obj, obj.pg, pgTo, sideVid, isKick)
}