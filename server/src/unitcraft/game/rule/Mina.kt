package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.injectValue
import unitcraft.server.Side

class Mina(r: Resource) {
    val flats: () -> Flats by injectFlats()

    init {
        val tile = r.tile("mina")
        val mover = injectValue<Mover>()
        val lifer = injectValue<Lifer>()
        injectValue<Editor>().onEdit(PriorDraw.flat, listOf(tile), { pg, side, num -> plant(pg, side) }, { pg ->
            val flat = flats()[pg]
            if (flat.has<Mina>()) {
                flat.remove<Mina>()
                true
            } else false
        })
        injectValue<Flater>().slotDrawFlat.add(3,this,"рисует мину") {
            if(side == flat.orNull<Mina>()?.side) ctx.drawTile(flat.pg, tile)
        }
        mover.slotMoveAfter.add { move ->
            val flat = flats()[move.pgTo]
            if (flat.has<Mina>()) {
                flat.remove<Mina>()
                lifer.damage(move.obj, 1)
                true
            } else false
        }
    }

    fun plant(pg: Pg, side: Side) {
        flats()[pg].data(Mina(side))
    }

    operator fun contains(pg: Pg) = flats()[pg].has<Mina>()

    class Mina(val side: Side) : Data
}
