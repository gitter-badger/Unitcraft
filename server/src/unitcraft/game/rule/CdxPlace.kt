package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.game.Place.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game
import java.util.*

class CdxPlace(r:Resource) : Cdx(r){
    val tiles = values().map{it to r.tlsList(sizeFix[it]!!, it.name(),Resource.effectPlace)}.toMap()

    override fun createRules(land: Land,g: Game) = rules{
        val places = Grid<Place>()
        val fixs = Grid<Map<Place, Int>>()
        val hide : MutableSet<Any> = Collections.newSetFromMap(WeakHashMap<Any,Boolean>())

        for (pg in land.pgser.pgs) {
            places[pg] = land.place(pg)
            fixs[pg] = land.fixs(pg)
        }

        info<MsgDraw>(0) {
            for (pg in g.pgs) {
                val place = places[pg]
                drawTile(pg, tiles[place]!![fixs[pg]!![place]!!])
            }
        }

        info<MsgIsHided>(0){
            if(voin in hide) yes()
        }

        for(place in values())
            editAdd(place.ordinal(),tiles[place]!!.first()) {
                places[pgEdit] = place
            }

        make<EfkUnhide>(0){
            hide.remove(voin)
        }

        endTurn(5) {
            // скрыть врагов в лесу
            for ((pg,place) in places) if (place == Place.forest){
                val voin = g.info(MsgVoin(pg)).voin
                if(voin!=null) {
                    val efk = EfkHide(pg, g.sideTurn.vs(), voin)
                    if (!g.stop(efk)) hide.add(voin)
                }
            }
        }
    }

    companion object  {
        val sizeFix:Map<Place,Int> = mapOf(
                forest to 4,
                grass to 5,
                hill to 1,
                mount to 1,
                sand to 4,
                water to 1
        )
    }
}