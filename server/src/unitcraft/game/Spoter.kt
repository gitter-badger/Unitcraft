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

    val skils = HashMap<Kind, List<Skil>>()
    val addSkils = ArrayList<(Obj)->List<Skil>>()
    val stopSkils = ArrayList<(Obj,Skil)->Boolean>()

    fun spot(pgSpot: Pg, sideVid: Side): List<Sloy> {
        val sloys = ArrayList<Sloy>()
        for(obj in objs().byPg(pgSpot).byKind(skils.keySet()).sortBy{it.shape.zetOrder}){
            for(skil in skils[obj.kind].plus(addSkils.flatMap{it(obj)}).filterNot { skil -> stopSkils.any{it(obj,skil)} }){
                val r =  Raise(pgSpot,true)
                pgSpot.near.forEach {
                    r.add(it,skil.tlsAkt()){}
                }
                sloys.addAll(r.sloys())
            }
        }

//        for (onRaise in onRaises) {
//            val sideSpot = onRaise.sideSpot(pgSpot)
//            if(sideSpot!=null) {
//                val isOn = stager.sideTurn() == sideVid && (sideVid == sideSpot || false/*enforced*/)
//                val spot = Spot(pgSpot, isOn)
//                spot.addRaise()
//                onRaise.spot(armer, pgSpot, pgSpot, sideVid, spot)
//                for (pgSrc in onRaise.spotByCopy(pgSpot)) {
//                    spot.addRaise()
//                    onRaises.forEach { onRaise.spot(armer, pgSpot, pgSrc, sideVid, spot) }
//                }
//                sloys.addAll(spot.sloys())
//            }
//        }
        return sloys
    }
}

interface Skil{
    fun tlsAkt():TlsAkt
}

interface OnRaise {
    fun sideSpot(pg: Pg): Side?
    fun spot(arm: Arm, pgSpot: Pg, pgSrc: Pg, sideVid: Side, s: Spot){}
    fun spotByCopy(pgSpot:Pg): List<Pg> = emptyList()
}

interface Arm {
    fun canMove(move: Move): (()->Boolean)?
    fun canSkil(pgFrom: Pg, pgTo: Pg, side: Side): Boolean
    fun canSell(pgFrom: Pg, pgTo: Pg): Boolean
    fun canEnforce(pgFrom: Pg, pgTo: Pg): Boolean
    fun canDmg(pgFrom: Pg, pgTo: Pg): Boolean
}

class Spot(val pgSpot: Pg, val isOn: Boolean) {
    val raises = ArrayList<Raise>()

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, fn: () -> Unit) {
        raises.last().add(pgAkt, tlsAkt, fn)
    }

    fun addRaise() {
        raises.add(Raise(pgSpot, isOn))
    }

    fun sloys(): List<Sloy> {
        // TODO схлопнуть, если нет пересечений
        return raises.flatMap { it.sloys() }
    }
}

class Raise(val pgRaise: Pg, val isOn: Boolean) {
    private val listSloy = ArrayList<Sloy>()

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, fn: () -> Unit) {
        addAkt(Akt(pgAkt, tlsAkt(isOn), null, fn))
    }
    //    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        if (akt.pgAim == pgRaise) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}

class PreAkt(val pg:Pg,tlsAkt: TlsAkt, fn: () -> Unit)