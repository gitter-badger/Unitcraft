package unitcraft.game

import unitcraft.game.rule.AllData
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList

class Stager(r:Resource) {
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

    fun stage(sideVid:Side):Stage{
        if(sideTurn()==sideVid) return Stage.turn
        else if(sideTurn()==sideVid.vs) return Stage.turnEnemy
        else throw Err("stage assertion")
    }

    fun edge(sideVid:Side):DabTile{
        return when(stage(sideVid)){
            Stage.turn -> tileEdgeTurn
            Stage.turnEnemy -> tileEdgeWait
            else -> tileEdgeWait
        }
    }
}