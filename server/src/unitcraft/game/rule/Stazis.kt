package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.server.exclude

class Stazis(r: Resource) {
    val tiles = r.tlsList(5, "stazis")
    val stager: Stager by inject()
    val editor: Editor by inject()
    val drawer: Drawer by inject()
    val spoter: Spoter by inject()
    val mover: Mover by inject()
    val flats: () -> Flats by injectFlats()

    init {
        editor.onEdit(PriorDraw.overObj,listOf(tiles.last()), { pg, side, num -> plant(pg) }, { pg ->
            val flat = flats()[pg]
            if(flat.has<Stazis>()){
                flat.remove<Stazis>()
                true
            }else false
        })
        stager.onEndTurn {
            for ((flat,stazis) in flats().by<Stazis,Flat>()) {
                stazis.value -= 1
                if(stazis.value==0) flat.remove<Stazis>()
            }
        }
        drawer.onDraw(PriorDraw.overObj) { side, ctx ->
            for ((flat,stazis) in flats().by<Stazis,Flat>()) ctx.drawTile(flat.head(), tiles[stazis.value - 1])
        }
        mover.slotStopMove.add{ it.shapeTo.pgs.any{it in this} }
        spoter.slotStopSkils.add{ obj,skil -> obj.shape.pgs.all{it !in this}}
    }

    fun plant(pg: Pg) {
        flats()[pg].data(Stazis())
    }

    operator fun contains(pg:Pg) = flats()[pg].has<Stazis>()

    class Stazis:Data{
        var value = 5
    }
}


