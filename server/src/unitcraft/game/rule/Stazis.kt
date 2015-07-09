package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.exclude

class Stazis(r: Resource, val stager: Stager, val editor: Editor, val drawer: Drawer, val grid: () -> Grid<Int>) {
    val tiles = r.tlsList(5, "stazis")

    init {
        editor.onEdit(listOf(tiles.last()), { pg, side, num -> plant(pg) }, { pg -> grid().remove(pg) != null })
        stager.onEndTurn {
            for ((pg, v) in grid()) grid()[pg] = v - 1
            grid().values().exclude { it == 0 }
        }
        drawer.onDraw(PriorDraw.overSky) { side, ctx ->
            for ((pg, num) in grid()) ctx.drawTile(pg, tiles[num - 1])
        }
    }

    fun plant(pg: Pg) {
        grid()[pg] = 5
    }
}