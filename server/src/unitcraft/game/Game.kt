package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.*
import java.util.ArrayList
import java.util.HashMap

class Game(val pgser: Pgser, canEdit: Boolean = false, val drawer:Drawer,val editor:Editor?,val raiser:Raiser,val stager: Stager) {
    val pgs = pgser.pgs

    val traces = Traces()

    fun cmd(side: Side, cmd: String) {
        if(side.isN) throw throw Err("side is neutral")
        if (cmd.isEmpty()) throw Violation("cmd is empty")
        val prm = Prm(pgser, cmd[1, cmd.length()].toString())
        when (cmd[0]) {
            'z' -> editAdd(side, prm)
            'r' -> editRemove(prm)
            'd' -> editDestroy(prm)
            'c' -> editChange(side, prm)
            'a' -> akt(side, prm)
            'b' -> aktOpt(side, prm)
            'e' -> endTurn(side, prm)
            else -> throw Violation("unknown msg: " + cmd)
        }
    }

    fun state(): GameState {
        return GameState(null, Side.values().map { it to snap(it).toJson() }.toMap(), null)
    }

    fun cmdRobot(): String? {
        return if (stager.sideTurn() == Side.b) "e" else null
    }

    fun land(): String {
        throw UnsupportedOperationException()
    }

    private fun editAdd(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(3)
        val num = prm.int(2)
        if(num >= editor!!.opterTest.opts.size()) throw Violation("editAdd out bound")
        editor.editAdd(prm.pg(0),side,num)
    }

    private fun editRemove(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        editor!!.editRemove(prm.pg(0))
    }

    private fun editDestroy(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        editor!!.editDestroy(prm.pg(0))
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        editor!!.editChange(prm.pg(0), side)
    }

    private fun akt(side: Side, prm: Prm) {
        prm.ensureSize(5)
        val sloy = raiser.spot(prm.pg(0),side)[prm.int(2)]
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3)) ?: throw Violation("akt not found")
        traces.clear()
        akt.fn?.invoke()
        println("akt " + side + " from " + prm.pg(0) + " index " + prm.int(2) + " to " + prm.pg(3))
    }

    private fun aktOpt(side: Side, prm: Prm) {
        prm.ensureSize(6)
        val sloy = raiser.spot(prm.pg(0),side)[prm.int(2)]
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3)) ?: throw Violation("akt not found")
        traces.clear()
        //make(akt.efkOpt) prm.int(5)
        println("akt " + side + " from " + prm.pg(0) + " index " + prm.int(2) + " to " + prm.pg(3) + " opt " + prm.int(5))
    }

    private fun endTurn(side: Side, prm: Prm) {
        prm.ensureSize(0)
        if (stager.sideTurn() != side) throw Violation("endTurn side($side) != sideTurn")
        stager.endTurn()
    }

    private fun snap(side: Side) = Snap(
            pgser.xr,
            pgser.yr,
            drawer.draw(side),
            pgs.map{it to raiser.spot(it, side)}.filter{it.second.isNotEmpty()}.toMap(),
            traces, side == stager.sideTurn(), Stage.turn, editor?.opterTest
    )
    private fun ensureTest() {
        if (editor == null) throw Violation("only for test game")
    }
}