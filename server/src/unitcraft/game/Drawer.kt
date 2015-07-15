package unitcraft.game

import unitcraft.game.rule.Kind
import unitcraft.game.rule.Obj
import unitcraft.game.rule.Objs
import unitcraft.game.rule.Singl
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class Drawer(val objs: () -> Objs) {

    private val draws = HashMap<PriorDraw, MutableList<(Side, CtxDraw) -> Unit>>()
//    private val drawObjs = HashMap<Kind, (Obj, Side, CtxDraw) -> Unit>()
//
//    private val tiles = HashMap<Kind, Int>()
//
//    fun addTile(kind: Kind, tile: Int) {
//        tiles[kind] = tile
//    }
//
//    fun onDrawObj(kind: Kind, drawObj: (Obj, Side, CtxDraw) -> Unit) {
//        drawObjs[kind] = drawObj
//    }

    fun onDraw(prior: PriorDraw, onDraw: (Side, CtxDraw) -> Unit) {
        draws.getOrPut(prior) { ArrayList<(Side, CtxDraw) -> Unit>() }.add(onDraw)
    }

    fun draw(side: Side): List<DabOnGrid> {
        val ctx = CtxDraw(side)
        for (prior in PriorDraw.values()) {
            draws[prior]?.forEach { it(side, ctx) }
//            objs().filter { it.priorDraw == prior }.p.forEach { obj ->
//                val drawObj = drawObjs[obj.kind]
//                if (drawObj != null) {
//                    drawObj(obj, side, ctx)
//                } else {
//                    val shape = obj.shape
//                    when (shape) {
//                        is Singl -> ctx.drawTile(shape.pg, tile(obj))
//                    }
//                }
//            }
        }
        return ctx.dabOnGrids
    }

    //private fun tile(obj: Obj) = tiles[obj.kind] ?: tileDflt

}

enum class PriorDraw {
    place, flat, underVoin, voin, overVoin, fly, overFly
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