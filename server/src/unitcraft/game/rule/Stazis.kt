package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.exclude

class Stazis(r: Resource, val stager: Stager, val editor: Editor, val drawer: Drawer,val spoter: Spoter, mover: Mover, private val flats: () -> Flats) {
    val tiles = r.tlsList(5, "stazis")

    init {
        editor.onEdit(PriorDraw.overObj,listOf(tiles.last()), { pg, side, num -> plant(pg) }, { pg -> flats()[pg].remove<Stazis>() != null })
        stager.onEndTurn {
            for ((flat,stazis) in flats().by<Stazis>()) {
                stazis.value -= 1
                if(stazis.value==0) flat.remove<Stazis>()
            }
        }
        drawer.onDraw(PriorDraw.overObj) { side, ctx ->
            for ((flat,stazis) in flats().by<Stazis>()) ctx.drawTile(flat.head(), tiles[stazis.value - 1])
        }
        mover.slotStopMove.add{ it.shapeTo.pgs.any{it in this} }
        spoter.slotStopSkils.add{ obj,skil -> obj.shape.pgs.all{it !in this}}
    }

    fun plant(pg: Pg) {
        flats()[pg].data(Stazis())
    }

    fun contains(pg:Pg) = flats()[pg].has<Stazis>()

    class Stazis:Data{
        var value = 5
    }
}


