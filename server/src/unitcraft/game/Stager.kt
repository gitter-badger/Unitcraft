package unitcraft.game

import unitcraft.game.rule.AllData
import unitcraft.game.rule.Flag
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Stager(r: Resource) {
    val allData: () -> AllData by injectAllData()
    val flag by inject<Flag>()
    val tileEdgeTurn = DabTile(r.tile("edgeTurn", Resource.effectPlace))
    val tileEdgeWait = DabTile(r.tile("edgeWait", Resource.effectPlace))

    val focus = DabTile(r.tile("focus"))

    private val endTurns = ArrayList<(Side) -> Unit>()
    private val startTurns = ArrayList<(Side) -> Unit>()

    fun onEndTurn(fn: (Side) -> Unit) = endTurns.add(fn)
    fun onStartTurn(fn: (Side) -> Unit) = startTurns.add(fn)

    fun sideTurn() = allData().sideTurn

    fun endTurn() {
        val sideTurn = allData().sideTurn
        endTurns.forEach { it(sideTurn) }
        allData().sideTurn = sideTurn.vs
        startTurns.forEach { it(sideTurn.vs) }
        checkWin()
    }

    fun stage(sideVid: Side) = when {
        allData().sideWin == sideVid -> Stage.win
        allData().sideWin == sideVid.vs -> Stage.winEnemy
        allData().bonus[sideVid] == null -> Stage.bonus
        allData().bonus[sideVid.vs] == null -> Stage.bonusEnemy
        allData().needJoin && sideTurn() == sideVid -> Stage.join
        allData().needJoin && sideTurn() == sideVid.vs -> Stage.joinEnemy
        sideTurn() == sideVid -> Stage.turn
        sideTurn() == sideVid.vs -> Stage.turnEnemy
        else -> throw Err("stage assertion")
    }

    fun edge(sideVid: Side): DabTile {
        return when (stage(sideVid)) {
            Stage.turn,Stage.join,Stage.bonus -> tileEdgeTurn
            else -> tileEdgeWait
        }
    }

    fun isBeforeTurn(sideVid: Side) = stage(sideVid).ordinal <= 3

    fun isTurn(sideVid: Side) = stage(sideVid) == Stage.turn

    private fun checkWin(){
        allData().sideWin = if(Side.ab.all{allData().objs.bySide(it).isEmpty()}) flag.sideMost()
        else Side.ab.firstOrNull{allData().point[it] == 0 || allData().objs.bySide(it).isEmpty()}?.vs
    }
}