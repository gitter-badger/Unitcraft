package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class SkilerMove(r: Resource){
    init{
        val tlsAktMove = r.tlsAktMove
        val mover = injectValue<Mover>()
        val spoter = injectValue<Spoter>()
        spoter.listOnTire.add{obj ->
            if(obj.has<SkilMove>()) obj<SkilMove>().refuel()
        }
        spoter.addSkil<SkilMove> { side, obj, objSrc ->
            val data = objSrc<SkilMove>()
            val list = ArrayList<AktSimple>()
            if(data.fuel>0) for(pg in obj.shape.head.near) {
                val move = Move(obj, obj.shape.headTo(pg), side)
                val can = mover.canMove(move)
                if (can!=null) list.add(AktSimple(pg, tlsAktMove) {
                    if(can()) {
                        mover.move(move)
                        if(obj==objSrc) data.fuel -= 1
                    }
                })
            }
            list
        }
    }
}

class SkilMove(var fuelMax:Int = 3):Data{
    var fuel = fuelMax

    fun refuel(){
        fuel = fuelMax
    }
}

class SkilerHit(r:Resource){
    init{
        val tlsAkt = r.tlsAkt("hit")
        val lifer = injectValue<Lifer>()
        val spoter =  injectValue<Spoter>()
        spoter.addSkil<DataHit>(){sideVid, obj, objSrc ->
            val data = objSrc<DataHit>()
            obj.near().filter { lifer.canDamage(it) }.map {
                AktSimple(it, tlsAkt) {
                    lifer.damage(it,data.dmg)
                    spoter.tire(obj)
                }
            }
        }
    }
}

class DataHit(val dmg:Int) : Data
