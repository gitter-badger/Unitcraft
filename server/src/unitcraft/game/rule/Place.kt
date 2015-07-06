package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.game.TpPlace.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game
import java.util.*

class Place(val pgser:()->Pgser,val tiles: Map<TpPlace, List<Int>>,val grid:() -> Grid<TpPlace>,val fixs:() -> Grid<Map<TpPlace, Int>>):OnDraw,OnEdit{
    override val prior = OnDraw.Prior.place
    override val tilesEditAdd = TpPlace.values().map{tiles[it]!!.first()}

    override fun draw(side: Side, ctx: CtxDraw) {
        for (pg in pgser()) {
            val tp = grid()[pg]
            ctx.drawTile(pg, tiles[tp]!![fixs()[pg][tp]!!])
        }
    }

    override fun editAdd(pg: Pg, side: Side,num:Int) {
        grid()[pg] = TpPlace.values()[num]
    }

    override fun editRemove(pg: Pg) = false
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