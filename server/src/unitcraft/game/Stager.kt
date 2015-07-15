package unitcraft.game

import unitcraft.game.rule.Obj
import unitcraft.game.rule.Objs
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class Stager(val objs: () -> Objs) {
    private val endTurns = ArrayList<(Side) -> Unit>()

    fun onEndTurn(fn: (Side) -> Unit) = endTurns.add(fn)

    fun sideTurn() = objs().sideTurn

    fun endTurn() {
        endTurns.forEach { it(objs().sideTurn) }

        objs().sideTurn = objs().sideTurn.vs
    }
}