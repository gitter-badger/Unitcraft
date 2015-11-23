package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Side

class Mina(r: Resource) {
    val flats: () -> Flats by injectFlats()

    init {
        val tile = r.tile("mina")
        val mover = injectValue<Mover>()
        val lifer = injectValue<Lifer>()
        injectValue<Editor>().onEdit(PriorDraw.flat,listOf(tile), { pg, side, num -> plant(pg,side) }, { pg ->
            val flat = flats()[pg]
            if(flat.has<Mina>()){
                flat.remove<Mina>()
                true
            }else false
        })
        injectValue<Drawer>().onDraw(PriorDraw.overObj) { side, ctx ->
            for ((flat,mina) in flats().by<Mina, Flat>()) if(mina.side==side)
                ctx.drawTile(flat.head(), tile)
        }
        mover.slotMoveAfter.add{shapeFrom,move ->
            var abort = false
            move.shapeTo.pgs.forEach{
                val flat = flats()[it]
                if(flat.has<Mina>()){
                    flat.remove<Mina>()
                    lifer.damage(move.obj,1)
                    abort = true
                }
            }
            abort
        }
    }

    fun plant(pg: Pg,side:Side) {
        flats()[pg].data(Mina(side))
    }

    operator fun contains(pg:Pg) = flats()[pg].has<Mina>()

    class Mina(val side: Side): Data
}
