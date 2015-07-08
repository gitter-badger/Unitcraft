package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.*

class Drawer(val pgser: () -> Pgser,val objs:()-> Objs) {

    val draws = HashMap<PriorDraw, MutableList<(Side, CtxDraw) -> Unit>>()

    val tiles = HashMap<Kind,Int>()
    val fnTiles = HashMap<Kind,(Obj)->Int>()

    fun onDraw(prior:PriorDraw,onDraw:(Side,CtxDraw)->Unit){
        draws.getOrPut(prior){ArrayList<(Side, CtxDraw) -> Unit>()}.add(onDraw)
    }



    fun draw(side:Side): List<DabOnGrid> {
        val ctx = CtxDraw(side)
        for(prior in PriorDraw.values()){
            draws[prior]?.forEach { it(side,ctx) }
            objs().filter { it.priorDraw == prior }.forEach { obj ->
                val shape = obj.shape
                when(shape){
                    is Singl -> ctx.drawTile(shape.pg, tile(obj))
                }
            }
        }
        return ctx.dabOnGrids
    }

    private fun tile(obj:Obj) = fnTiles[obj.kind]?.invoke(obj)?:tiles[obj.kind]?:0

}

enum class PriorDraw{
    place, flat, underVoin, voin, overVoin
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