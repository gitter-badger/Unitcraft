package unitcraft.game

import unitcraft.game.rule.AllData
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Stager(r: Resource) {
    val allData: () -> AllData by injectAllData()
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
        allData().point[sideTurn] = allData().point[sideTurn]!! - 1
        allData().sideTurn = sideTurn.vs
        startTurns.forEach { it(sideTurn.vs) }
    }

    fun stage(sideVid: Side) = when {
        allData().point[sideVid] == 0 -> Stage.winEnemy
        allData().point[sideVid.vs] == 0 -> Stage.win
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
            Stage.turn -> tileEdgeTurn
            Stage.turnEnemy -> tileEdgeWait
            else -> tileEdgeWait
        }
    }

    fun isBeforeTurn(sideVid: Side) = stage(sideVid).ordinal <= 3

    fun isTurn(sideVid: Side) = stage(sideVid) == Stage.turn
}