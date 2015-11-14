package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Side
import java.util.*
import kotlin.reflect.KClass

class Drawer {
    val allData: () -> AllData  by injectAllData()
    private val draws = HashMap<PriorDraw, MutableList<(Side, CtxDraw) -> Unit>>()

    val tileWithGroundFlat = HashMap<KClass<out Data>,(Flat, Data, Side) -> Pair<Tile,Tile?>?>()
    val drawFlats = ArrayList<(Flat, Side, CtxDraw) -> Unit>()
    val drawObjs = ArrayList<(Obj, Side,CtxDraw) -> Unit>()

    fun onDraw(prior: PriorDraw, onDraw: (Side, CtxDraw) -> Unit) {
        draws.getOrPut(prior) { ArrayList<(Side, CtxDraw) -> Unit>() }.add(onDraw)
    }

    fun draw(side: Side): List<DabOnGrid> {
        val ctx = CtxDraw(side)
        for (prior in PriorDraw.values) {
            drawFlats(side,ctx)
            draws[PriorDraw.flat]?.forEach { it(side, ctx) }
            drawObjs(side, ctx)
            draws[PriorDraw.obj]?.forEach { it(side, ctx) }
            draws[PriorDraw.overObj]?.forEach { it(side, ctx) }
        }
        return ctx.dabOnGrids
    }

    private fun drawFlats(side: Side,ctx: CtxDraw){
        for (flat in allData().flats.sort()) {
            for (data in flat.datas) {
                val fn = tileWithGroundFlat[data.javaClass.kotlin]
                if(fn!=null) {
                    val gt = fn(flat, data, side)
                    if (gt != null) {
                        ctx.drawTile(flat.head(), gt.first)
                        gt.second?.let { ctx.drawTile(flat.head(), it) }
                        break
                    }
                }
            }
            drawFlats.forEach{it(flat,side,ctx)}
        }
    }

    private fun drawObjs(side: Side, ctx: CtxDraw) {
        for ((obj,ht) in allData().objs.sort().by<HasTileObj,Obj>()) {
            ctx.drawTile(obj.head(), ht.tile(side,obj), ht.hint(side,obj))
            drawObjs.forEach{it(obj,side,ctx)}
        }
    }

    inline fun <reified D:Data> onFlat(noinline tileWithGround:(Flat,D, Side) -> Pair<Tile,Tile?>?){
        tileWithGroundFlat[D::class] = tileWithGround as (Flat, Data, Side) -> Pair<Tile,Tile?>?
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

interface HasTileObj : Data{
    fun tile(sideVid: Side, obj: Obj):Tile
    fun hint(sideVid: Side, obj: Obj):Int?
}

//interface HasTileFlat : Data{
//    fun tile(sideVid: Side, flat: Flat):Tile?
//    fun ground(sideVid: Side, flat: Flat):Tile
//}