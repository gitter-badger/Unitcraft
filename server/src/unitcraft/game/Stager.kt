package unitcraft.game

import unitcraft.game.rule.AllData
import unitcraft.game.rule.Obj
import unitcraft.game.rule.Objs
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class Stager(val allData: () -> AllData) {
    private val endTurns = ArrayList<(Side) -> Unit>()

    fun onEndTurn(fn: (Side) -> Unit) = endTurns.add(fn)

    fun sideTurn() = allData().sideTurn

    fun endTurn() {
        val sideTurn = allData().sideTurn
        endTurns.forEach { it(sideTurn) }
        allData().point[sideTurn] -= 1
        allData().sideTurn = sideTurn.vs
    }


}