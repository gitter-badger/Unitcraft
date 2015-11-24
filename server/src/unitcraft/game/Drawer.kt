package unitcraft.game

import unitcraft.game.rule.AllData
import unitcraft.game.rule.Data
import unitcraft.game.rule.Flat
import unitcraft.game.rule.Obj
import unitcraft.server.Side
import java.util.*

class Drawer(r: Resource) {
    val allData: () -> AllData  by injectAllData()

    val ground = r.tileGround
    val tileFlatNull = r.tile("null.flat")
    val tileObjNull = r.tile("null.obj")

    fun draw(side: Side): List<DabOnGrid> {
        val ctx = CtxDraw(side)
        for (prior in PriorDraw.values) {
            drawFlats(side, ctx)
            draws[PriorDraw.flat]?.forEach { it(side, ctx) }
            drawObjs(side, ctx)
            draws[PriorDraw.obj]?.forEach { it(side, ctx) }
            draws[PriorDraw.overObj]?.forEach { it(side, ctx) }
        }
        return ctx.dabOnGrids
    }

    private fun drawFlats(side: Side, ctx: CtxDraw) {
        for (flat in allData().flats.sort()) {
            val gt = tileWithGroundFlat.map { it(flat, side) }.firstOrNull { it != null }
            if (gt != null) {
                ctx.drawTile(flat.pg, gt.first)
                gt.second?.let { ctx.drawTile(flat.pg, it) }
            } else {
                ctx.drawTile(flat.pg, ground)
                ctx.drawTile(flat.pg, tileFlatNull)
            }
            drawFlats.forEach { it(flat, side, ctx) }
        }
    }

    private fun drawObjs(side: Side, ctx: CtxDraw) {
        for (obj in allData().objs.sort()) {
            val th = tileWithHintObj.map { it(obj, side) }.firstOrNull { it != null }
            if (th != null)
                ctx.drawTile(obj.pg, th.first, th.second)
            else
                ctx.drawTile(obj.pg, tileObjNull)
            drawObjs.forEach { it(obj, side, ctx) }
        }
    }

    val drawFlats = ArrayList<(Flat, Side, CtxDraw) -> Unit>()
    val drawObjs = ArrayList<(Obj, Side, CtxDraw) -> Unit>()

    private val draws = HashMap<PriorDraw, MutableList<(Side, CtxDraw) -> Unit>>()

    fun onDraw(prior: PriorDraw, onDraw: (Side, CtxDraw) -> Unit) {
        draws.getOrPut(prior) { ArrayList<(Side, CtxDraw) -> Unit>() }.add(onDraw)
    }

    val tileWithGroundFlat = ArrayList<(Flat, Side) -> Pair<Tile, Tile?>?>()

    inline fun <reified D : Data> onFlat(noinline tileWithGround: (Flat, D, Side) -> Pair<Tile, Tile?>) {
        tileWithGroundFlat.add { flat, side -> if (flat.has<D>()) tileWithGround(flat, flat<D>(), side) else null }
    }

    val tileWithHintObj = ArrayList<(Obj, Side) -> Pair<Tile, HintTile?>?>()

    inline fun <reified D : Data> onObj(noinline tileWithHint: (Obj, D, Side) -> Pair<Tile, HintTile?>) {
        tileWithHintObj.add { obj, side -> if (obj.has<D>()) tileWithHint(obj, obj<D>(), side) else null }
    }
}

enum class PriorDraw {
    flat, obj, overObj
}

class CtxDraw(val sideVid: Side) {
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