package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class Drawer(r:Resource,val hider:Hider,val pgser:()->Pgser,val allData: () -> AllData) {

    private val draws = HashMap<PriorDraw, MutableList<(Side, CtxDraw) -> Unit>>()

    val drawFlats = ArrayList<(Flat, Side,CtxDraw) -> Unit>()
    val drawObjs = ArrayList<(Obj, Side,CtxDraw) -> Unit>()

    fun onDraw(prior: PriorDraw, onDraw: (Side, CtxDraw) -> Unit) {
        draws.getOrPut(prior) { ArrayList<(Side, CtxDraw) -> Unit>() }.add(onDraw)
    }

    fun draw(side: Side): List<DabOnGrid> {
        val ctx = CtxDraw(side)
        for (prior in PriorDraw.values()) {
            drawFlats(side,ctx)
            draws[PriorDraw.flat]?.forEach { it(side, ctx) }
            drawObjs(side, ctx)
            draws[PriorDraw.obj]?.forEach { it(side, ctx) }
            draws[PriorDraw.overObj]?.forEach { it(side, ctx) }
        }
        return ctx.dabOnGrids
    }

    private fun drawFlats(side: Side,ctx: CtxDraw){
        val flats = allData().flats
        for (pg in pgser()) {
            val flat = flats[pg]
            drawFlats.forEach{it(flat,side,ctx)}
        }
    }

    private fun drawObjs(side: Side, ctx: CtxDraw) {
        for (obj in allData().objs) {
            drawObjs.forEach{it(obj,side,ctx)}
        }
    }

}

enum class PriorDraw {
    flat, obj, overObj
}

class CtxDraw(val sideVid: Side) {
    val dabOnGrids = ArrayList<DabOnGrid>()

    fun drawTile(pg: Pg, tile: Tile, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabTile(tile, hint)))
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabText(text, hint)))
    }

    fun drawText(pg: Pg, value: Int, hint: Int? = null) {
        drawText(pg, value.toString(), hint)
    }
}