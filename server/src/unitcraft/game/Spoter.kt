package unitcraft.game

import org.json.simple.JSONObject
import unitcraft.game.rule.*
import unitcraft.inject.inject
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.Violation
import java.util.*

class Spoter(r:Resource) {
    val hintTileAktOff = r.hintTileAktOff
    val stager: Stager  by inject()
    val allData:()-> AllData  by injectAllData()

    val listCanAkt = ArrayList<(Side,Obj)->Boolean>()
    val listSkil = ArrayList<(Obj)->((Side,Obj,Obj) -> List<Akt>)?>()
    val listSkilByCopy = ArrayList<(Obj)->Obj?>()

    val listOnTire = ArrayList<(Obj) -> Unit>()
    val slotStopSkils = ArrayList<(Obj,Data)->Boolean>()

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
        val obj = objs()[pgFrom]?:throw Violation("obj not found")
        val sloy = sloysObj(obj,sideVid).elementAtOrNull(index)?: throw Violation("sloy not found")
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(pgAkt) ?: throw Violation("akt not found")
        if(num==null) {
            if (akt !is AktSimple) throw Violation("akt=$akt !is AktSimple")
            startAkt(obj, sideVid)
            akt.fn()
            //obj.lastSequel = akt.fn()
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

    private fun skilsOwnObj(obj:Obj) = listSkil.map{it(obj)}.filterNotNull().map{obj to it}//.filter{skil -> slotStopSkils.any{it(obj,skil)}}

    private fun skilsObj(obj:Obj)=
        skilsOwnObj(obj) + listSkilByCopy.map{it(obj)}.filterNotNull().flatMap{skilsOwnObj(it)}


    private fun sloysObj(obj:Obj,sideVid:Side):List<Sloy>{
        val isOn = if(stager.isTurn(sideVid) && obj.isFresh) listCanAkt.any{it(sideVid,obj)} else false
        val r =  Raise(obj.shape.pgs,isOn, hintTileAktOff)
        for((objSrc,skil) in skilsObj(obj))
            for(akt in skil(sideVid,obj,objSrc))
                r.addAkt(akt)
        return r.sloys()
    }

    fun tire(obj:Obj){
        obj.isFresh = false
        listOnTire.forEach{it(obj)}
    }

    inline fun <reified D:Data> addSkil(noinline akts:(Side,Obj,Obj) -> List<Akt>){
        listSkil.add{obj -> if(obj.has<D>()) akts else null}
    }
}

interface Sequel{
    fun akts(sideVid: Side,obj:Obj):List<Akt>
}

class Raise(val pgsErr: List<Pg>, val isOn: Boolean,val hintTileAktOff:HintTile) {
    private val listSloy = ArrayList<Sloy>()

    fun addAkt(akt: Akt) {
        if(akt is AktOpt && akt.dabs.isEmpty()) return
        if (akt.pg in pgsErr) throw Err("self-cast not implemented: akt at ${akt.pg}")
        val idx = listSloy.indexOfLast { it.aktByPg(akt.pg) != null } + 1
        if (idx == listSloy.size) listSloy.add(Sloy(isOn,hintTileAktOff))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}
