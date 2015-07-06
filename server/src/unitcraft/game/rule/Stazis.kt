package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import unitcraft.server.idxsMap

class Stazis(r:Resource,val grid:() -> Grid<Int>):OnDraw,OnEdit{
    val tiles = r.tlsList(5,"stazis")

    fun plant(pg: Pg) {
        grid()[pg] = 5
    }

    fun decoy(pg: Pg) {
        val num = grid()[pg]!!
        if (num > 1) grid()[pg] = num - 1
        else grid().remove(pg)
    }

    override val prior = OnDraw.Prior.overVoin

    override fun draw(side: Side, ctx: CtxDraw) {
        for ((pg, num) in grid()) ctx.drawTile(pg, tiles[num - 1])
    }

    override val tileEditAdd = tiles.last()

    override fun editAdd(pg: Pg, side: Side) {
        plant(pg)
    }

    override fun editRemove(pg: Pg) = grid().remove(pg)

    //        r.after<EfkEndTurn>(0){
    //            grid().forEach { decoy(it.key) }
    //        }
}
