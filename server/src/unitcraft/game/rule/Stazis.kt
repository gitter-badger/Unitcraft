package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.exclude

class Stazis(r: Resource) {


    val flats: () -> Flats by injectFlats()

    init {
        val tiles = r.tlsList(5, "stazis")
        injectValue<Editor>().onEdit(PriorDraw.overObj,listOf(tiles.last()), { pg, side, num -> plant(pg) }, { pg ->
            val flat = flats()[pg]
            if(flat.has<Stazis>()){
                flat.remove<Stazis>()
                true
            }else false
        })
        injectValue<Stager>().onEndTurn {
            for ((flat,stazis) in flats().by<Stazis,Flat>()) {
                stazis.value -= 1
                if(stazis.value==0) flat.remove<Stazis>()
            }
        }
        injectValue<Drawer>().onDraw(PriorDraw.overObj) { side, ctx ->
            for ((flat,stazis) in flats().by<Stazis,Flat>()) ctx.drawTile(flat.pg, tiles[stazis.value - 1])
        }

        val mover = injectValue<Mover>()
        mover.slotStopMove.add{ it.pgTo in this || it.pgFrom in this }
        mover.slotStopAdd.add{ pg, side -> pg in this }

        injectValue<Spoter>().slotStopSkils.add{ obj -> obj.pg in this }
        injectValue<Lifer>().slotStopDamage.add{ obj -> obj.pg in this }
    }

    fun plant(pg: Pg) {
        flats()[pg].orPut { Stazis() }
    }

    operator fun contains(pg:Pg) = flats()[pg].has<Stazis>()

    class Stazis:Data{
        var value = 5
    }
}


