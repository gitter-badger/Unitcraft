package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class SkilerMove(r: Resource){
    val tlsAktMove = r.tlsAktMove
    val mover: Mover  by inject()

    init{
        injectValue<Spoter>().listOnTire.add{obj ->
            if(obj.has<SkilMove>()) obj<SkilMove>().refuel()
        }
    }

    fun add(obj:Obj,fuelMax:Int){
        obj.data(SkilMove(fuelMax, mover,tlsAktMove))
    }

    class SkilMove(var fuelMax:Int,val mover: Mover,val tlsAktMove:TlsAkt):Skil{
        var fuel = fuelMax

        fun refuel(){
            fuel = fuelMax
        }

        override fun akts(sideVid: Side, obj: Obj): List<AktSimple> {
            val list = ArrayList<AktSimple>()
            if(fuel>0) for(pg in obj.shape.head.near) {
                val move = Move(obj, obj.shape.headTo(pg), sideVid)
                val can = mover.canMove(move)
                if (can!=null) list.add(AktSimple(pg, tlsAktMove) {
                    if(can()) {
                        mover.move(move)
                        fuel -= 1
                    }
                })
            }
            return list
        }
    }
}

class SkilerHit(r:Resource){
    val tlsAkt = r.tlsAkt("hit")
    val lifer:Lifer by inject()
    val spoter: Spoter by inject()

    fun add(obj:Obj){
        obj.data(SkilHit())
    }

    inner class SkilHit() : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.near().filter { lifer.canDamage(it) }.map {
                    AktSimple(it, tlsAkt) {
                        lifer.damage(it,1)
                        spoter.tire(obj)
                    }
                }
    }
}

