package unitcraft.game

import unitcraft.server.Err
import unitcraft.server.Side

class Armer() {
//    val stopAim = exts.filterIsInstance<OnStopAim>()
//    val arm = exts.filterIsInstance<OnArm>()
//    val getBusys = exts.filterIsInstance<OnGetBusy>()

//    override fun canMove(move: Move): (()->Boolean)? {
//        if (stopAim.any { it.stop(move) }) return null
//        val busy = getBusy(move.pgTo, move.tpMoveTo, move.sideVid)
//        if(busy.isEmpty()) return {true}
//        val reveals = busy.map{it.reveal}.filterNotNull()
//        return { reveals.forEach { it() };false }
//    }
//
//    private fun getBusy(pg: Pg, tpMove: TpMove, side: Side): List<Busy> {
//        return getBusys.map { it.getBusy(pg, tpMove, side) }.filterNotNull()
//    }
//
//    override fun canSell(pgFrom: Pg, pgTo: Pg): Boolean {
//        return stopAim.all { !it.stopSkil(pgFrom, pgTo) }
//    }
//
//    override fun canEnforce(pgFrom: Pg, pgTo: Pg): Boolean {
//        throw UnsupportedOperationException()
//    }
//
//    override fun canDmg(pgFrom: Pg, pgTo: Pg): Boolean {
//        throw UnsupportedOperationException()
//    }
//
//    override fun canSkil(pgFrom: Pg, pgTo: Pg,side:Side): Boolean {
//        return true
//    }
}

interface OnStopAim {
    fun stop(move: Move) = false
    fun stopSkil(pgFrom: Pg, pgTo: Pg) = false
}

interface OnArm {

}

class Move(
        val pgFrom: Pg,
        val pgTo: Pg,
        val tpMoveTo: TpMove,
        val isIgnoreStop: Boolean,
        val sideVid: Side
)


enum class TpMove {
    flat, unit, fly
}

interface OnGetBusy {
    fun getBusy(pg: Pg, tpMove: TpMove, side: Side): Busy?
}

class Busy(val reveal: (() -> Unit)? = null)