package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.game.TpPlace.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import java.util.*

class Place(val pgser:()->Pgser,val tiles: Map<TpPlace, List<Tile>>,val grid:() -> Grid<TpPlace>,val fixs:() -> Grid<Map<TpPlace, Int>>,val drawer:Drawer,val editor:Editor){
    init{
        drawer.onDraw(PriorDraw.place){side, ctx ->
            for (pg in pgser()) {
                val tp = grid()[pg]
                ctx.drawTile(pg, tiles[tp]!![fixs()[pg]!![tp]!!])
            }
        }

        editor.onEdit(PriorDraw.place,TpPlace.values().map{tiles[it]!!.first()},{pg,side,num -> grid()[pg] = TpPlace.values()[num]},{false})
    }

    fun start(land: Land) {
        for ((pg, v) in land.grid()) grid().set(pg, v)
        for ((pg, v) in land.fixs()) fixs().set(pg, v)
    }
}
//val hide : MutableSet<Any> = Collections.newSetFromMap(WeakHashMap<Any,Boolean>())
//        endTurn(5) {
// скрыть врагов в лесу
//            for ((pg,place) in places) if (place == TpPlace.forest){
//                 g.info(MsgVoin(pg)).all.forEach{
//                    val efk = EfkHide(pg, g.sideTurn.vs, it)
//                    if (!g.stop(efk)) hide.add(it)
//                }
//            }
//        }