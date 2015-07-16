package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class SkilerMove(r: Resource, spoter: Spoter,val mover: Mover){
    val tlsAktMove = r.tlsAktMove
    val kinds = ArrayList<Kind>()

    init{
        spoter.listOnTire.add{obj ->
            if(obj.has<SkilMove>()) obj<SkilMove>().refuel()
        }
    }

    fun getSkil(fuelMax:Int):Data{
        return SkilMove(fuelMax, mover,tlsAktMove)
    }

    class SkilMove(var fuelMax:Int,val mover: Mover,val tlsAktMove:TlsAkt):Data(),Skil{
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

//class SkilMove(val tls:TlsAkt): Skil {
//    override fun preAkts(pgSpot:Pg,sideVid: Side,obj:Obj): List<PreAkt> {
//        pgSpot.near.map{PreAkt(pg,tls){
//            i
//        }}
//    }
//}
//
//class SkilMove(r:Resource,val shaper: Shaper){
//    val tlsMove = r.tlsAktMove
//    fun pgs(pgSpot:Pg,obj:Obj,sideVid:Side,r:Raise){
//        for(pg in pgSpot.near) {
//            val shapeTo = obj.shape.copy(head=pg)
//            val can = shaper.canMove(Move(obj, shapeTo, sideVid))
//            if (can!=null)
//                r.add(pg, tlsMove) {
//                    if(can()) obj.shape = shapeTo
//                }
//        }
//    }
//}


