package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.Violation
import java.util.*

class Spoter(val stager: Stager,val objs:()-> Objs) {
    private val freshed = "freshed"
    val listCanAkt = ArrayList<(Side,Obj)->Boolean>()
    val listSkil = ArrayList<(Obj)->Skil?>()
    val listSkilByCopy = ArrayList<(Obj)->List<Obj>>()
    val listOnTire = ArrayList<(Obj) -> Unit>()
    val slotStopSkils = ArrayList<(Obj,Skil)->Boolean>()

    init{
        stager.onEndTurn {
            objs().objAktLast = null
            objs().forEach { refresh(it) }
        }
    }

    fun spots(sideVid: Side): Map<Pg,List<Sloy>>{
        val spots = HashMap<Pg,ArrayList<Sloy>>()
        for(zetOrder in ZetOrder.reverse){
            for(obj in objs().byZetOrder(zetOrder)){
                val sloysObj = sloysObj(obj,sideVid)
                if(sloysObj.isNotEmpty()) for(pg in obj.shape.pgs){
                    spots.getOrPut(pg){ArrayList<Sloy>()}.addAll(sloysObj)
                }
            }
        }
        return spots
    }

    fun akt(sideVid:Side,pgFrom: Pg,index:Int,pgAkt: Pg,num:Int? = null){
        var sm = 0
        var objFinded:Obj? = null
        var sloyFinded: Sloy? = null
        for(zetOrder in ZetOrder.reverse){
            val obj = objs().byPg(pgFrom).byZetOrder(zetOrder).firstOrNull()?:continue
            val sloysObj = sloysObj(obj,sideVid)
            if(index-sm<sloysObj.size()) {
                objFinded = obj
                sloyFinded = sloysObj[index-sm]
            }else{
                sm += sloysObj.size()
            }
        }
        if(sloyFinded==null || objFinded==null) throw Violation("sloy not found")
        if (!sloyFinded.isOn) throw Violation("sloy is off")
        val akt = sloyFinded.aktByPg(pgAkt) ?: throw Violation("akt not found")
        if(num==null) {
            if (akt !is AktSimple) throw Violation("akt=$akt !is AktSimple")
            startAkt(objFinded, sideVid)
            akt.fn()
            endAkt(objFinded, sideVid)
        }else{
            if (akt !is AktOpt) throw Violation("akt=$akt !is AktOpt")
            if(num !in akt.dabs.indices) throw Violation("num=$num is out range for opt akt=$akt")
            startAkt(objFinded, sideVid)
            akt.fn(num)
            endAkt(objFinded, sideVid)
        }
    }

    private fun endAkt(obj:Obj,sideVid: Side){
        if(isFresh(obj) && sloysObj(obj,sideVid).isEmpty()) tire(obj)
        if(isFresh(obj)) objs().objAktLast = obj
    }

    private fun startAkt(obj:Obj,sideVid: Side){
//        objs().objAktLast?.let{ if(obj!=it) tire(it) }
    }

    private fun skilsOwnObj(obj:Obj)=listSkil.map{it(obj)}.filterNotNull().filter{skil -> slotStopSkils.any{it(obj,skil)}}

    private fun skilsObj(obj:Obj)=
        (skilsOwnObj(obj) + listSkilByCopy.flatMap{it(obj)}.flatMap{skilsOwnObj(it)}).distinct()


    private fun sloysObj(obj:Obj,sideVid:Side):List<Sloy>{
        val sloys = ArrayList<Sloy>()
        for(skil in skilsObj(obj)){
            val isOn = if(sideVid == stager.sideTurn() && isFresh(obj)) listCanAkt.any{it(sideVid,obj)} else false
            val r =  Raise(obj.shape.pgs,isOn)
            for(p in skil.akts(sideVid,obj))
                r.addAkt(p)
            sloys.addAll(r.sloys())
        }
        return sloys
    }

    fun isFresh(obj: Obj) = (obj[freshed] as Boolean?) ?: false

    fun refresh(obj: Obj) {
        obj[freshed] = true
    }

    fun tire(obj:Obj){
        obj[freshed] = false
        listOnTire.forEach{it(obj)}
    }
}

interface Skil{
    fun akts(sideVid: Side,obj:Obj):List<Akt>
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
        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}
