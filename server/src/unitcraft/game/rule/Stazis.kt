package unitcraft.game.rule

import unitcraft.game.EfkEndTurn
import unitcraft.game.Grid
import unitcraft.game.Pg
import unitcraft.game.Rules

class Stazis : Listener{
    private val stazis = Grid<Int>()

    fun plant(pg: Pg) {
        stazis[pg] = 5
    }

    fun decoy(pg: Pg) {
        val num = stazis[pg]!!
        if (num > 1) stazis[pg] = num - 1
        else stazis.remove(pg)
    }

    override fun register(r: Rules) {
        r.after<EfkEndTurn>(0){
            stazis.forEach { decoy(it.key) }
        }
    }
}

interface Listener{
    fun register(r: Rules)
}
