package unitcraft.game

import unitcraft.server.Side
import unitcraft.server.Violation
import java.util.*

class Stager(val score:()->Score){
    fun sideTurn() = score().sideTurn

    fun endTurn(){
        score().sideTurn = score().sideTurn.vs
    }
}

class Score{
    var sideTurn: Side = Side.a
    val bonus = HashMap<Side, Int>()
    val point = HashMap<Side, Int>()
}