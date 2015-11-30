package unitcraft.game

import unitcraft.game.rule.Obj
import unitcraft.game.rule.Objs
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Mover(r: Resource) {
    val stager: Stager by inject()
    val tracer: Tracer by inject()

    val objs: () -> Objs by injectObjs()
    val slotStopMove = ArrayList<(Move) -> Boolean>()
    val slotStopAdd = ArrayList<(Pg, Side) -> Boolean>()
    val slotMoveAfter = ArrayList<(Move) -> Boolean>()
    val slotHide = ArrayList<(Obj) -> Boolean>()

    val tileReveal = r.tile("reveal")

    init {
        stager.onStartTurn { side -> objs().bySide(side).forEach { hideIfNeed(it) } }
    }


    /**
     * Перемещает объект, если это возможно.
     * null - движение недоступно
     * ()->Boolean - совершает движение и выполняет ifMove, или если невидимка в pgTo, то раскрывает его
     */
    fun move(obj: Obj, pgTo: Pg, sideVid: Side): (() -> Boolean)? {
        val mr = moveResult(obj, pgTo, sideVid, false)
        return if (mr == null) null else {
            val fn = { mr().passed };fn
        }
    }

    fun movePath(obj: Obj, path: List<Pg>, sideVid: Side) {
        for (p in path) {
            val mr = moveResult(obj, p, sideVid)?.invoke() ?: break
            if (mr.passed) {
                if (mr.isInterrupt) break
            } else break
        }
    }

    fun kick(obj: Obj, pgTo: Pg, sideVid: Side): (() -> Boolean)? {
        val mr = moveResult(obj, pgTo, sideVid, true)
        return if (mr == null) null else {
            val fn = { mr().passed };fn
        }
    }

    fun kickPath(obj: Obj, path: List<Pg>, sideVid: Side) {
        for (p in path) {
            val mr = moveResult(obj, p, sideVid, true)?.invoke() ?: break
            if (!mr.passed) break
        }
    }

    fun jumpAll(jumps: List<Pair<Obj, Pg>>) {
        for ((obj, pg) in jumps) obj.pg = pg
        if (objs().distinctBy { it.pg }.size < objs().list.size) throw Err("obj clash after jumpAll")
        watch()
    }

    fun isMove(obj: Obj, pgFrom: Pg, pgTo: Pg, sideVid: Side): Boolean {
        val move = Move(obj, pgFrom, pgTo, sideVid, false)
        return if (slotStopMove.any { it(move) }) false else {
            !(objs()[pgTo]?.isVid(sideVid) ?: false)
        }
    }

    fun isMove(obj: Obj, pgTo: Pg, sideVid: Side) =
            isMove(obj, obj.pg, pgTo, sideVid)

    fun isKick(obj: Obj, pgFrom: Pg, pgTo: Pg, sideVid: Side): Boolean {
        val move = Move(obj, pgFrom, pgTo, sideVid, true)
        return if (slotStopMove.any { it(move) }) false else {
            !(objs()[pgTo]?.isVid(sideVid) ?: false)
        }
    }

    /**
     * Добавляет объект на поле, если это возможно.
     * null - движение недоступно
     * ()->Unit - совершает движение и выполняет ifMove, или еслив pgTo невидимка, то раскрывает его
     */
    fun canAdd(pg: Pg, sideVid: Side, ifAdd: (Obj, Int) -> Unit): ((Int) -> Unit)? {
        if (slotStopAdd.any { it(pg, sideVid) }) return null
        val obj = objs()[pg] ?: return { num ->
            Obj(pg).apply {
                ifAdd(this, num)
                if (objs()[this.pg] != null) throw Err("obj=$this clash")
                objs().list.add(this)
                hideIfNeed(this)
                watch()
            }
        }
        return if (obj.isVid(sideVid)) null else {
            val fn = { num: Int -> reveal(obj) };fn
        }
    }

    private fun moveResult(obj: Obj, pgTo: Pg, sideVid: Side, isKick: Boolean = false): (() -> ResultMove)? {
        val move = Move(obj, obj.pg, pgTo, sideVid, isKick)
        if (slotStopMove.any { it(move) }) return null
        val objDest = objs()[pgTo] ?: return { ResultMove(true, doMove(move)) }
        return if (objDest.isVid(sideVid)) null else {
            val fn = { reveal(objDest);ResultMove(false, false) };fn
        }
    }

    private fun doMove(move: Move): Boolean {
        jumpAll(listOf(move.obj to move.pgTo))
        return slotMoveAfter.map { it(move) }.any { it }
    }

    private fun watch() {
        for (side in Side.ab) objs().filter { !it.isVid(side) && isWatched(it) }.forEach { reveal(it) }
    }

    private fun isWatched(obj: Obj) = obj.pg.near.any { objs()[it]?.let { it.side == obj.side.vs } ?: false }

    fun hideIfNeed(obj: Obj) {
        if (!isWatched(obj) && slotHide.any { it(obj) }) obj.hide = true
    }

    fun revealUnhided() {
        objs().forEach { obj ->
            if (!slotHide.any { it(obj) }) reveal(obj)
        }
    }

    fun reveal(obj: Obj) {
        if (obj.hide) {
            obj.hide = false
            tracer.touch(obj.pg, tileReveal)
        }
    }

    fun remove(obj: Obj) {
        objs().list.remove(obj)
    }
}

class ResultMove(val passed: Boolean, val isInterrupt: Boolean)

/**
 * Событие изменения позиции объекта.
 * если isKick, значит объект перемещается не сам
 */
class Move(val obj: Obj, val pgFrom: Pg, val pgTo: Pg, val sideVid: Side, val isKick: Boolean = false) {
    //constructor(obj: Obj, pgTo: Pg, sideVid: Side, isKick: Boolean = false) : this(obj, obj.pg, pgTo, sideVid, isKick)
}