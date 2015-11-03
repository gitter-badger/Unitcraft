package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.Violation
import java.util.*

class Spoter {
    val stager: Stager  by inject()
    val allData:()-> AllData  by inject()
    val listCanAkt = ArrayList<(Side,Obj)->Boolean>()
    val listSkil = ArrayList<(Obj)->Skil?>()
    val listSkilByCopy = ArrayList<(Obj)->List<Obj>>()
    val listOnTire = ArrayList<(Obj) -> Unit>()
    val slotStopSkils = ArrayList<(Obj,Skil)->Boolean>()

    init{
        stager.onEndTurn {
            allData().objAktLast = null
            objs().forEach { it.isFresh = true }
        }
    }

    fun objs() = allData().objs

    fun spots(sideVid: Side): Map<Pg,List<Sloy>>{
        val spots = HashMap<Pg,ArrayList<Sloy>>()
        for(obj in objs()){
            val sloysObj = sloysObj(obj,sideVid)
            if(sloysObj.isNotEmpty()) for(pg in obj.shape.pgs){
                spots.getOrPut(pg){ArrayList<Sloy>()}.addAll(sloysObj)
            }
        }
        return spots
    }

    fun akt(sideVid:Side,pgFrom: Pg,index:Int,pgAkt: Pg,num:Int? = null){
        var sm = 0

        val obj = objs()[pgFrom]?:throw Violation("obj not found")
        val sloy = sloysObj(obj,sideVid).elementAtOrNull(index)?: throw Violation("sloy not found")
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(pgAkt) ?: throw Violation("akt not found")
        if(num==null) {
            if (akt !is AktSimple) throw Violation("akt=$akt !is AktSimple")
            startAkt(obj, sideVid)
            akt.fn()
            endAkt(obj, sideVid)
        }else{
            if (akt !is AktOpt) throw Violation("akt=$akt !is AktOpt")
            if(num !in akt.dabs.indices) throw Violation("num=$num is out range for opt akt=$akt")
            startAkt(obj, sideVid)
            akt.fn(num)
            endAkt(obj, sideVid)
        }
    }

    private fun endAkt(obj:Obj,sideVid: Side){
        if(obj.isFresh && sloysObj(obj,sideVid).isEmpty()) tire(obj)
        if(obj.isFresh) allData().objAktLast = obj
    }

    private fun startAkt(obj:Obj,sideVid: Side){
        allData().objAktLast?.let{ if(obj!=it) tire(it) }
    }

    private fun skilsOwnObj(obj:Obj)=(obj.get<Skil>()+listSkil.map{it(obj)}.filterNotNull()).filter{skil -> slotStopSkils.any{it(obj,skil)}}

    private fun skilsObj(obj:Obj)=
        (skilsOwnObj(obj) + listSkilByCopy.flatMap{it(obj)}.flatMap{skilsOwnObj(it)}).distinct()


    private fun sloysObj(obj:Obj,sideVid:Side):List<Sloy>{
        val isOn = if(sideVid == stager.sideTurn() && obj.isFresh) listCanAkt.any{it(sideVid,obj)} else false
        val r =  Raise(obj.shape.pgs,isOn)
        for(skil in skilsObj(obj))
            for(p in skil.akts(sideVid,obj))
                r.addAkt(p)
        return r.sloys()
    }

    fun tire(obj:Obj){
        obj.isFresh = false
        listOnTire.forEach{it(obj)}
    }
}

interface Skil:Data{
    fun akts(sideVid: Side,obj:Obj):List<Akt>
}

class Raise(val pgsErr: List<Pg>, val isOn: Boolean) {
    private val listSloy = ArrayList<Sloy>()

//    fun add(pgAkt: Pg, tlsAkt: TlsAkt, fn: () -> Unit) {
//        addAkt(AktSimple(pgAkt, tlsAkt, fn))
//    }
//
//    fun add(pgAim: Pg, tlsAkt: TlsAkt,dabs:List<List<Dab>>, fn: (Int) -> Unit){
//        addAkt(AktOpt(pgAim, tlsAkt, dabs, fn))
//    }

    fun addAkt(akt: Akt) {
        if(akt is AktOpt && akt.dabs.isEmpty()) return
        //if (akt.pgAim in pgsErr) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pg) != null } + 1
        if (idx == listSloy.size) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}
