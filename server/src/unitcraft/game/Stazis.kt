package unitcraft.game

import unitcraft.game.*
import unitcraft.server.Side
import unitcraft.server.idxsMap

class Stazis(val grid:() -> Grid<Int>){

    fun plant(pg: Pg) {
        grid()[pg] = 5
    }

    fun decoy(pg: Pg) {
        val num = grid()[pg]!!
        if (num > 1) grid()[pg] = num - 1
        else grid().remove(pg)
    }
//        r.after<EfkEndTurn>(0){
//            grid().forEach { decoy(it.key) }
//        }
}

