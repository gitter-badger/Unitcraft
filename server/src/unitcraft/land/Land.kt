package unitcraft.land

import java.util.ArrayList
import kotlin.properties.Delegates
import java.util.HashMap
import unitcraft.game.*
import unitcraft.game.rule.Singl
import unitcraft.server.*
import unitcraft.land.Random
import unitcraft.land.TpFlat.*
import unitcraft.land.TpSolid.*

class Land(maxTpFlat:Map<TpFlat,Int>, maxTpSolid:Map<TpSolid,Int>,val mission: Int?){

    val seed = mission?.toLong() ?: System.nanoTime()
    val r = Random(seed)
    val mapFlat = maxTpFlat.mapValues { r.nextInt(it.value) }
    val yr = 9 + r.nextInt(7)
    val xr = if(yr==15) 15 else yr + r.nextInt(15-yr)
    val pgser = Pgser(xr,yr)
    val xl = xr-1
    val yl = yr-1

    val flats = ArrayList<Flat>()
    val solids = ArrayList<Solid>()

    init {
        for(pg in pgser) {
            addFlat(pg, none, 0)
        }

        Algs.spot(this).forEach {
            addFlat(it, wild, 0)
        }

        Algs.spot(this).forEach {
            addFlat(it, wild, 1)
        }

        Algs.spot(this).forEach {
            addFlat(it, flag, 0)
        }

        solids.add(Solid(pgser.pg(0,3),builder,0))
        solids.add(Solid(pgser.pg(xl,yl-3),builder,1))
    }

    fun addFlat(pg:Pg,tpFlat:TpFlat,idx:Int){
        flats.idxOfFirst() { it.pg == pg }?.let{ flats.remove(it) }
        flats.add(Flat(pg, tpFlat, idx))
    }

    fun pgRnd() = selRnd(pgser.pgs)

    fun pgRnd(predicate:(Pg) -> Boolean):Pg{
        val list = pgser.pgs.filter(predicate)
        if(list.isEmpty()) throw Err("predicate nevewhere true")
        return selRnd(list)
    }

    fun selRnd<E>(list:List<E>) = if(!list.isEmpty()) list[r.nextInt(list.size())] else throw Err("list is empty")

    fun isEdge(pg:Pg) = pg.x==0 || pg.x==xl || pg.y==0 || pg.y==yl

    fun isCorner(pg:Pg) = pg.x==0 && pg.y==0 || pg.x==xl && pg.y==0 || pg.x==xl && pg.y==yl || pg.x==0 && pg.y==yl

}

enum class TpFlat{
    none, solid, liquid, wild, special, flag
}

enum class TpSolid{
    std, builder
}

class Flat(val pg:Pg,val tpFlat:TpFlat,val num:Int){
    val shape = Singl(pg)
}
class Solid(val pg:Pg,val tpSolid:TpSolid,val num:Int){
    val shape = Singl(pg)
}

class PrmAlg(val land:Land,val pgs:MutableList<Pg>){
    fun add(pg:Pg){
        pgs.add(pg)
    }

    fun pgRnd() = land.selRnd(land.pgser.pgs)
}

fun createAlg(fn:PrmAlg.()->Unit):(Land)->List<Pg>{
    return {land:Land ->
        val lst = ArrayList<Pg>()
        val p = PrmAlg(land,lst)
        p.fn()
        lst
    }
}

object Algs{
    val spot = createAlg{
       repeat(5){
           add(pgRnd())
       }
    }
}