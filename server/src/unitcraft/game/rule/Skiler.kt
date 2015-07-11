package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import java.util.*

class SkilerMove(r: Resource, spoter: Spoter,val shaper: Shaper):Skil{
    val tlsAktMove = r.tlsAktMove
    val kinds = ArrayList<Kind>()

    init{
        spoter.listSkils.add{ if(it.kind in kinds) listOf(this) else emptyList() }
    }

    fun charged(obj: Obj): Boolean {
        return (obj["move.charge"] as Boolean?) ?: true
    }

    fun discharge(obj: Obj) {
        obj["move.charge"] = false
    }

    private fun fuel(obj:Obj):Int{
        return (obj["move.fuel"] as Int?)?:3
    }

    private fun minusFuel(obj:Obj) {
        val fl = fuel(obj) - 1
        if (fl == 0) {
            obj["move.fuel"] = 3
            discharge(obj)
        } else {
            obj["move.fuel"] = fl
        }
    }

    override fun isReady(obj: Obj) = fuel(obj) > 0

    override fun preAkts(sideVid: Side, obj: Obj): List<PreAkt> {
        val list = ArrayList<PreAkt>()
        for(pg in obj.shape.head.near) {
            val move = Move(obj, obj.shape.headTo(pg), sideVid)
            val can = shaper.canMove(move)
            if (can!=null) list.add(PreAkt(pg, tlsAktMove) {
                if(can()) {
                    shaper.move(move)
                    minusFuel(obj)
                }
            })
        }
        return list
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


