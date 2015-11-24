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
    val listSkil = ArrayList<(Obj)->((Side,Obj) -> Akts)?>()

    val listOnTire = ArrayList<(Obj) -> Unit>()
    val slotStopSkils = ArrayList<(Obj)->Boolean>()

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
            if(sloysObj.isNotEmpty())
                spots.getOrPut(obj.pg){ArrayList<Sloy>()}.addAll(sloysObj)
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
        allData().objAktLast = if(obj in objs() && obj.isFresh)
            if (sloysObj(obj, sideVid).isEmpty()){
                tire(obj)
                null
            } else obj
        else null
    }

    private fun startAkt(obj:Obj,sideVid: Side){
        allData().objAktLast?.let{ if(obj!=it) tire(it) }
    }

    private fun skilsObj(obj:Obj) = listSkil.map{it(obj)}.filterNotNull().filterNot{slotStopSkils.any{it(obj)}}

    private fun sloysObj(obj:Obj,sideVid:Side):List<Sloy>{
        val isOn = if(stager.isTurn(sideVid) && obj.isFresh) listCanAkt.any{it(sideVid,obj)} else false
        val r = Raise(obj.pg,isOn, hintTileAktOff)
        val listAkts = skilsObj(obj).map{it(sideVid,obj)}
        val aktsModal = listAkts.firstOrNull(){it.isModal}
        if(aktsModal!=null){
            for(akt in aktsModal.list)
                r.addAkt(akt)
        }else{
            for(akts in listAkts)
                for(akt in akts.list)
                    r.addAkt(akt)
        }
        return r.sloys()
    }

    fun tire(obj:Obj){
        obj.isFresh = false
        listOnTire.forEach{it(obj)}
    }

    fun pgFocus() = allData().objAktLast?.pg

    inline fun <reified D:Data> addSkil(noinline akts:(Side,Obj) -> List<Akt>){
        val fn = {side:Side,obj:Obj ->
            val h = Akts(side,obj)
            h.list.addAll(akts(side,obj))
            h
        }
        listSkil.add{obj -> if(obj.has<D>()) fn else null}
    }

    inline fun <reified D:Data> addSkilByBuilder(noinline build: Akts.() -> Unit){
        val fn = {side:Side,obj:Obj ->
            val h = Akts(side,obj)
            h.build()
            h
        }
        listSkil.add{obj -> if(obj.has<D>()) fn else null}
    }
}

class Akts(val sideVid: Side, val obj:Obj){
    val list = ArrayList<Akt>()
    var isModal = false
    private set

    fun modal(){
        isModal = true
    }

    fun akt(pg:Pg,tileAkt:Tile,fn:()->Unit){
        list.add(AktSimple(pg,tileAkt,fn))
    }

    fun aktOpt(pg:Pg,tileAkt:Tile,dabs:List<List<Dab>>,fn:(Int)->Unit){
        list.add(AktOpt(pg,tileAkt,dabs,fn))
    }
}

fun createSkil(build: Akts.() -> Unit)={ sideVid: Side, obj:Obj ->
    val h = Akts(sideVid,obj)
    h.build()
    h
}

class Raise(val pgErr: Pg, val isOn: Boolean,val hintTileAktOff:HintTile) {
    private val listSloy = ArrayList<Sloy>()

    fun addAkt(akt: Akt) {
        if(akt is AktOpt && akt.dabs.isEmpty()) return
        if (akt.pg == pgErr) throw Err("self-cast not implemented: akt at ${akt.pg}")
        val idx = listSloy.indexOfLast { it.aktByPg(akt.pg) != null } + 1
        if (idx == listSloy.size) listSloy.add(Sloy(isOn,hintTileAktOff))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}
