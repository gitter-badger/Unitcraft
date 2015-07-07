package unitcraft.game

import unitcraft.server.Side
import unitcraft.server.Violation
import java.util.*

class Stager(ext:List<Ext>,val score:()->Score){
    val endTurns = ext.filterIsInstance<OnEndTurn>()

    fun sideTurn() = score().sideTurn

    fun endTurn(){
        endTurns.forEach { it.onEndTurn(score().sideTurn) }
        score().sideTurn = score().sideTurn.vs
    }
}

class Score{
    var sideTurn: Side = Side.a
    val bonus = HashMap<Side, Int>()
    val point = HashMap<Side, Int>()
}

interface OnEndTurn{
    fun onEndTurn(side:Side)
}