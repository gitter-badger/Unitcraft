package unitcraft.game

import unitcraft.server.Side
import java.util.*

class Drawer(r:Resource) {
    val slotDraw = r.slot<CtxDraw>("Рисование")

    fun draw(side: Side): List<DabOnGrid> {
        val ctx = CtxDraw(side)
        slotDraw.exe(ctx)
        return ctx.dabOnGrids
    }
}

enum class PriorDraw {
    flat, obj, overObj
}

class CtxDraw(val side: Side):Aide {
    val dabOnGrids = ArrayList<DabOnGrid>()

    fun drawTile(pg: Pg, tile: Tile, hint: HintTile? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabTile(tile, hint)))
    }

    fun drawText(pg: Pg, text: String, hint: HintText? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabText(text, hint)))
    }

    fun drawText(pg: Pg, value: Int, hint: HintText? = null) {
        drawText(pg, value.toString(), hint)
    }
}