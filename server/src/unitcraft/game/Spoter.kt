package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Spoter(val stager: Stager,val objs:()-> Objs) {
    //val onRaises = exts.filterIsInstance<OnRaise>()
    //    fun spots(side: Side): Map<Pg,List<Sloy>> {
    //        val isOn = stager.sideTurn() == side
    //        return onRaises.flatMap{ ext -> ext.focus().map{
    //            it.first to Rais(ext.raise(aimer,it.first,it.first,it.second),it.first,isOn).sloys()
    //        }}.toMap().filter { it.value.isNotEmpty() }
    // 1) sideVid==sideFocus умолчание 2) sideTurn == sideVid 3) enforced разрешает
    //        if (breed.grid()[pgSrc] != null) breed.grid()[pgSpot]?.let { voinSpot ->
    //            if (!voinSpot.isHided) {
    //                if (pgSpot !in stazis.grid()) {
    //                    var isOn = voinSpot.side == side// || voinSpot
    //                    if (endTurn.sideTurn() != side) isOn = false
    //                    val r = Raise(pgSpot, isOn)
    //                    for (pgNear in pgSpot.near) aim(pgNear, pgSpot, breed, side, r)
    //                    for (pgNear in pgSpot.near) {
    //                        if (canMove(pgSpot, pgNear, voinSpot)) r.addFn(pgNear, tlsMove){
    //                            move(breed.grid(),pgSpot,pgNear)
    //                        }
    //                    }
    //                    raises.add(r)
    //                }
    //            }
    //        }
    //        return emptyMap()
    //    }

    //val skils = HashMap<Kind, List<Skil>>()
    val skils = ArrayList<Skil>()
    //val stopSkils = ArrayList<(Obj,Skil)->Boolean>()

    fun spots(sideVid: Side): Map<Pg,List<Sloy>>{
        val spots = HashMap<Pg,ArrayList<Sloy>>()
        for(zetOrder in ZetOrder.values()){
            for(obj in objs().byZetOrder(zetOrder)){
                val sloysObj = sloysObj(obj,sideVid)
                for(pg in obj.shape.pgs){
                    spots.getOrPut(pg){ArrayList<Sloy>()}.addAll(sloysObj)
                }
            }
        }
        return spots
    }

//    fun spot(pgSpot: Pg, sideVid: Side): List<Sloy> {
//        val sloys = ArrayList<Sloy>()
//        for(obj in objs().byPg(pgSpot)){//.byKind(skils.keySet()).sortBy{it.shape.zetOrder}){
//            //for(skil in skils[obj.kind].plus(addSkils.map{it(obj)}.filterNotNull()).filterNot { skil -> stopSkils.any{it(obj,skil)} }){
//            for(skil in skils){
//                val r =  Raise(pgSpot,skil.isReady(obj))
//                for(p in skil.preAkts(pgSpot,sideVid,obj)){
//                    r.add(p.pg,p.tlsAkt,p.fn)
//                }
//                sloys.addAll(r.sloys())
//            }
//        }
//        return sloys
//    }

    fun sloysObj(obj:Obj,sideVid:Side):List<Sloy>{
        val sloys = ArrayList<Sloy>()
        for(skil in skils){
            val r =  Raise(obj.shape.pgs,skil.isReady(obj))
            for(p in skil.preAkts(sideVid,obj)){
                r.add(p.pg,p.tlsAkt,p.fn)
            }
            sloys.addAll(r.sloys())
        }
        return sloys
    }
}

interface Skil{
    fun isReady(obj:Obj):Boolean
    fun preAkts(sideVid: Side,obj:Obj):List<PreAkt>
}

//class Spot(val pgSpot: Pg, val isOn: Boolean) {
//    val raises = ArrayList<Raise>()
//
//    fun add(pgAkt: Pg, tlsAkt: TlsAkt, fn: () -> Unit) {
//        raises.last().add(pgAkt, tlsAkt, fn)
//    }
//
//    fun addRaise() {
//        raises.add(Raise(pgSpot, isOn))
//    }
//
//    fun sloys(): List<Sloy> {
//        // TODO схлопнуть, если нет пересечений
//        return raises.flatMap { it.sloys() }
//    }
//}

class Raise(val pgsErr: List<Pg>, val isOn: Boolean) {
    private val listSloy = ArrayList<Sloy>()

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, fn: () -> Unit) {
        addAkt(Akt(pgAkt, tlsAkt(isOn), null, fn))
    }
    //    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        if (akt.pgAim in pgsErr) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}

class PreAkt(val pg:Pg,val tlsAkt: TlsAkt, val fn: () -> Unit)