package unitcraft.game

import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class Stager(val score: () -> Score) {
    private val endTurns = ArrayList<(Side) -> Unit>()

    fun onEndTurn(fn: (Side) -> Unit) = endTurns.add(fn)

    fun sideTurn() = score().sideTurn

    fun endTurn() {
        endTurns.forEach { it(score().sideTurn) }
        score().sideTurn = score().sideTurn.vs
    }
}

class Score {
    var sideTurn: Side = Side.a
    val bonus = HashMap<Side, Int>()
    val point = HashMap<Side, Int>()
}