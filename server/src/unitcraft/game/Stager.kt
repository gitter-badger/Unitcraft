package unitcraft.game

import unitcraft.game.rule.AllData
import unitcraft.game.rule.Hamlet
import unitcraft.game.rule.Flater
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side

class Stager(r: Resource) {
    val allData: () -> AllData by injectAllData()
    val flater by inject<Flater>()
    val tileEdgeTurn = DabTile(r.tile("edgeTurn", Resource.effectPlace))
    val tileEdgeWait = DabTile(r.tile("edgeWait", Resource.effectPlace))

    val focus = DabTile(r.tile("focus"))
    val lock = DabTile(r.tile("lock"))
    val focusMore = DabTile(r.tile("focusMore"))

    val slotTurnEnd = r.slot<AideSide>("Конец хода")

    fun sideTurn() = allData().sideTurn

    fun endTurn() {
        val sideTurn = allData().sideTurn
        slotTurnEnd.exe(AideSide(sideTurn))
        if(sideTurn.vs==allData().sideFirst) allData().qntTurnLeft -= 1
        allData().sideWin = checkSideWin()
        allData().sideTurn = sideTurn.vs
    }

    fun stage(sideVid: Side) = when {
        allData().sideWin == sideVid -> Stage.win
        allData().sideWin == sideVid.vs -> Stage.winEnemy
        allData().bonus[sideVid] == null -> Stage.bonus
        allData().bonus[sideVid.vs] == null -> Stage.bonusEnemy
        sideTurn() == sideVid -> Stage.turn
        sideTurn() == sideVid.vs -> Stage.turnEnemy
        else -> throw Err("stage assertion")
    }

    fun edge(sideVid: Side): DabTile {
        return when (stage(sideVid)) {
            Stage.turn, Stage.bonus -> tileEdgeTurn
            else -> tileEdgeWait
        }
    }

    fun isTurn(sideVid: Side) = stage(sideVid) == Stage.turn

    private fun checkSideWin() = if (Side.ab.all { allData().objs.bySide(it).isEmpty() } || allData().qntTurnLeft==0) sideMost()!! else null

    fun sideMost() = flater.sideMostPoint()?:allData().sideFirst
}

enum class Stage{
    bonus, bonusEnemy, turn, turnEnemy, win, winEnemy
}

class AideSide(val side: Side) : Aide