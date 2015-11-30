package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.injectValue

class Adhesive(r: Resource){
    val flats: () -> Flats by injectFlats()
    val objs: () -> Objs by injectObjs()

    init {
        val tile = r.tile("adhesive")
        val tileObj = r.tile("adhesive.obj")
        injectValue<Editor>().onEdit(PriorDraw.flat,listOf(tile), { pg, side, num -> plant(pg) }, { pg ->
            val flat = flats()[pg]
            if(flat.has<Adhesive>()){
                flat.remove<Adhesive>()
                true
            }else false
        })

        injectValue<Drawer>().onDraw(PriorDraw.flat) { side, ctx ->
            for ((flat,stazis) in flats().by<Adhesive,Flat>()) ctx.drawTile(flat.pg, tile)
        }

        injectValue<Drawer>().onDraw(PriorDraw.overObj) { side, ctx ->
            for ((flat,stazis) in flats().by<Adhesive,Flat>()) if(objs()[flat.pg]!=null) ctx.drawTile(flat.pg, tileObj)
        }

        injectValue<Mover>().slotStopMove.add{ !it.isKick && hasAdhesive(it.pgFrom) }
    }

    fun plant(pg: Pg) {
        flats()[pg].orPut { Adhesive }
    }

    object Adhesive:Data

    fun hasAdhesive(pg: Pg) = flats()[pg].has<Adhesive>()
}
