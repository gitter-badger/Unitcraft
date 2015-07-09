package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import unitcraft.server.idxsMap

class Stazis(r:Resource,val stager:Stager,val editor:Editor,val drawer:Drawer,val grid:() -> Grid<Int>){
    val tiles = r.tlsList(5,"stazis")

    init{
        editor.onEdit(tiles.last(),{pg, side -> plant(pg)},{pg -> grid().remove(pg)})
        stager.onEndTurn { grid().forEach { decoy(it.key) } }
        drawer.onDraw(PriorDraw.overSky){side, ctx ->
            for ((pg, num) in grid()) ctx.drawTile(pg, tiles[num - 1])
        }
    }

    fun plant(pg: Pg) {
        grid()[pg] = 5
    }

    private fun decoy(pg: Pg) {
        val num = grid()[pg]!!
        if (num > 1) grid()[pg] = num - 1
        else grid().remove(pg)
    }
}
