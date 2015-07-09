package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.game.TpPlace.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game
import java.util.*

class Place(val pgser:()->Pgser,val tiles: Map<TpPlace, List<Int>>,val grid:() -> Grid<TpPlace>,val fixs:() -> Grid<Map<TpPlace, Int>>,val drawer:Drawer,val editor:Editor){
    init{
        drawer.onDraw(PriorDraw.place){side, ctx ->
            for (pg in pgser()) {
                val tp = grid()[pg]
                ctx.drawTile(pg, tiles[tp]!![fixs()[pg][tp]!!])
            }
        }
        for(tp in TpPlace.values())
        editor.onEdit(tiles[tp]!!.first(),{pg,side -> grid()[pg] = tp},{false})
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