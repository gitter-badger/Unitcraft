package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.game.TpPlace.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game
import java.util.*

class Place(land:Land){
    val pgser = land.pgser
    val places = Grid<TpPlace>()
    val fixs = Grid<Map<TpPlace, Int>>()

    init{
        for (pg in pgser.pgs) {
            places[pg] = land.place(pg)
            fixs[pg] = land.fixs(pg)
        }
    }
}

class CdxPlace(r:Resource) : Cdx(r){
    val tiles = values().map{it to r.tlsList(sizeFix[it]!!, it.name(),Resource.effectPlace)}.toMap()

    override fun createRules(land: Land,g: Game) = rules{
        val places = Grid<TpPlace>()
        val fixs = Grid<Map<TpPlace, Int>>()
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
            for ((pg,place) in places) if (place == TpPlace.forest){
                 g.info(MsgVoin(pg)).all.forEach{
                    val efk = EfkHide(pg, g.sideTurn.vs, it)
                    if (!g.stop(efk)) hide.add(it)
                }
            }
        }
    }

    companion object  {
        val sizeFix:Map<TpPlace,Int> = mapOf(
                forest to 4,
                grass to 5,
                hill to 1,
                mount to 1,
                sand to 4,
                water to 1
        )
    }
}