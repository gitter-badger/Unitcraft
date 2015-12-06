package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.injectValue

class Adhesive(r: Resource){
    val flats: () -> Flats by injectFlats()
    val objs: () -> Objs by injectObjs()
    val slop = r.slop<AideAdhesive>("Объекты, которые игнорируют паутину")

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

        injectValue<Flater>().slotDrawFlat.add(5,this,"рисует паутину на земле") {
            if (flat.has<Adhesive>()) ctx.drawTile(flat.pg, tile)
        }

        injectValue<Objer>().slotDrawObjPost.add(20,this,"рисует паутину на объекте") {
            if(flats()[obj.pg].has<Adhesive>() && slop.pass(AideAdhesive(obj))) ctx.drawTile(obj.pg, tileObj)
        }

        injectValue<Mover>().slotStopMove.add{ !it.isKick && hasAdhesive(it.pgFrom) && slop.pass(AideAdhesive(it.obj)) }
    }

    fun plant(pg: Pg) {
        flats()[pg].orPut { Adhesive }
    }

    object Adhesive:Data

    fun hasAdhesive(pg: Pg) = flats()[pg].has<Adhesive>()
}

class AideAdhesive(val obj:Obj) : Aide