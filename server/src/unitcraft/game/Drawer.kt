package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.ArrayList

class Drawer(
        val pgser: () -> Pgser,
        val exts: List<Ext>
) {
    val draws = exts.filterIsInstance<OnDraw>().sortBy{it.prior}
    fun draw(side:Side): List<DabOnGrid> {
        val ctx = CtxDraw(Side.a)
        draws.forEach { it.draw(side,ctx) }
        return ctx.dabOnGrids
    }
}

interface OnDraw:Ext{
    val prior:Prior
    fun draw(side:Side,ctx: CtxDraw)

    enum class Prior{
        place, flat, underVoin, voin, overVoin
    }
}

class CtxDraw(val sideVid: Side) {
    val dabOnGrids = ArrayList<DabOnGrid>()

    fun drawTile(pg: Pg, tile: Int, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabTile(tile, hint)))
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabText(text, hint)))
    }

    fun drawText(pg: Pg, value: Int, hint: Int? = null) {
        drawText(pg,value.toString(),hint)
    }
}