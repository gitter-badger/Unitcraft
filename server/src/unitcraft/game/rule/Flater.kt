package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.game.TpPlace.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import java.util.*

class Flater(val pgser:()->Pgser,val tiles: Map<TpPlace, List<Tile>>,val flats:() -> Flats,val drawer:Drawer,val editor:Editor){
    init{
        drawer.onDraw(PriorDraw.place){side, ctx ->
            for (pg in pgser()) {
                val tp = flats()[pg].tpPlace
                ctx.drawTile(pg, tiles[tp]!![flats()[pg].fix[tp]!!])
            }
        }

        editor.onEdit(PriorDraw.place,TpPlace.values().map{tiles[it]!!.first()},{pg,side,num -> flats()[pg].tpPlace = TpPlace.values()[num]},{false})
    }

    fun start(land: Land) {

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