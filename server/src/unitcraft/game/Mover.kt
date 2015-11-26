package unitcraft.game

import unitcraft.game.rule.Obj
import unitcraft.game.rule.Objs
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Mover {
    val stager: Stager by inject()
    val objs: () -> Objs by injectObjs()
    val slotStopMove = ArrayList<(Move) -> Boolean>()
    val slotStopBuild = ArrayList<(Pg, Side) -> Boolean>()
    val slotMoveAfter = ArrayList<(Move) -> Boolean>()
    val slotHide = ArrayList<(Obj) -> Boolean>()

    init {
        stager.onStartTurn { side ->
            objs().forEach { obj ->
                if(obj.side==side) obj.hide = slotHide.any { it(obj) }
            }
        }
    }

    /**
     * Возвращает доступность движения.
     * null - движение недоступно
     * ()->Boolean - движение выглядит доступным: фунция возвращает true, если это правда
     */
    fun canMove(move: Move): (() -> Boolean)? {
        if (slotStopMove.any { it(move) }) return null
        return canBusy(move.pgTo, move.sideVid)
    }

    fun canBuild(pg: Pg, sideVid: Side): (() -> Boolean)? {
        if (slotStopBuild.any { it(pg, sideVid) }) return null
        return canBusy(pg, sideVid)
    }

    private fun canBusy(pg: Pg, sideVid: Side): (() -> Boolean)? {
        val obj = objs()[pg] ?: return { true }
        return if (obj.isVid(sideVid)) null else {
            val fn = { reveal(obj);false };fn
        }
    }

    // todo перенести его внутрь canMove
    fun move(move: Move): Boolean {
        if (move.obj.pg != move.pgFrom) throw Err("obj.pg(${move.obj.pg} != pgFrom(${move.pgFrom})")
        if (objs()[move.pgTo] != null) throw Err("cant move obj=${move.obj} to pg=${move.pgTo}")
        move.obj.pg = move.pgTo
        for (side in Side.ab) {
            val pgsWt = pgsWatch(side)
            objs().filter { !it.isVid(side) && it.pg in pgsWt }.forEach { it.hide = false }
        }
        return slotMoveAfter.map { it(move) }.any { it }
    }

    private fun pgsWatch(side: Side) = objs().bySide(side).flatMap { it.near() }.distinct()

    fun rehide() {
        objs().forEach { obj ->
            if (!slotHide.any { it(obj) }) obj.hide = false
        }
    }

    fun reveal(obj: Obj) {
        obj.hide = false
    }
}

class Move(val obj: Obj, val pgFrom: Pg, val pgTo: Pg, val sideVid: Side) {
    constructor(obj: Obj, pgTo: Pg, sideVid: Side) : this(obj, obj.pg, pgTo, sideVid)
}