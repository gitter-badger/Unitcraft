package unitcraft.land

import java.util.ArrayList
import kotlin.properties.Delegates
import java.util.HashMap
import unitcraft.game.*
import unitcraft.game.rule.Flat
import unitcraft.server.*
import unitcraft.land.Random
import kotlin.reflect.jvm.java

class Land(mapFlatAll:Map<TpFlat,List<(Flat)-> Unit>>,val mission: Int?){

    val seed = mission?.toLong() ?: System.nanoTime()
    val r = Random(seed)
    val mapFlat = mapFlatAll.mapValues { selRnd(it.value) }
    val yr = 9 + r.nextInt(7)
    val xr = if(yr==15) 15 else yr + r.nextInt(15-yr)
    val pgser = Pgser(xr,yr)
    val xl = xr-1
    val yl = yr-1
    val pgs: List<Pg> = ArrayList<Pg>().init {
        if (xr <= 0 || yr <= 0) throw Err("xr $xr <= 0 or yr $yr <= 0")
        for (xx in 0..xl) {
            for (yy in 0..yl) {
                val pg = Pg(this@Land, xx, yy)
                add(pg)
                pg(TpPlace.grass)
            }
        }
    }

    init {
        Algs.prod(this)
    }

    fun pg(x: Int, y: Int) = pgs[x * yr + y]

    fun pgOrNull(x: Int, y: Int) = if (isIn(x, y)) pg(x, y) else null

    fun pg(pg:unitcraft.game.Pg) = pg(pg.x,pg.y)

    fun place(pg:unitcraft.game.Pg) = TpPlace.valueOf(pg(pg.x,pg.y).tpPlace.name())

    fun isIn(x: Int, y: Int): Boolean {
        return x >= 0 && x < xr && y >= 0 && y < yr
    }

    fun pgRnd() = selRnd(pgs)

    fun pgRnd(predicate:(Pg) -> Boolean):Pg{
        val list = pgs.filter(predicate)
        if(list.isEmpty()) throw Err("predicate nevewhere true")
        return selRnd(list)
    }

    fun selRnd<E>(list:List<E>) = if(!list.isEmpty()) list[r.nextInt(list.size())] else throw Err("list is empty")

    fun isEdge(pg:Pg) = pg.x==0 || pg.x==xl || pg.y==0 || pg.y==yl

    fun isCorner(pg:Pg) = pg.x==0 && pg.y==0 || pg.x==xl && pg.y==0 || pg.x==xl && pg.y==yl || pg.x==0 && pg.y==yl

    fun grid():Grid<TpPlace> {
        val grid = Grid<TpPlace>()
        for (pg in pgser.pgs) grid[pg] = place(pg)
        return grid
    }

    fun flat(pg:unitcraft.game.Pg) = mapFlat[tpFlatFromTpPlace(pg(pg).tpPlace)]?:mapFlat[TpFlat.none]!!

    private fun tpFlatFromTpPlace(tp:TpPlace)=
        when(tp){
            TpPlace.grass -> TpFlat.none
            TpPlace.mount -> TpFlat.solid
            TpPlace.forest,TpPlace.sand,TpPlace.hill -> TpFlat.wild
            TpPlace.water -> TpFlat.liquid
        }

}

enum class TpPlace {
    grass, mount, forest, sand, hill, water
}

enum class TpFlat{
    none, solid, liquid, wild, special, flag
}

class Pg(val land:Land,val x:Int,val y:Int){
    var tpPlace: TpPlace = TpPlace.grass
//    var flat: TpFlat? = null
//    var skil: TpSkil? = null

    val near by Delegates.lazy { listOf(land.pgOrNull(x,y-1),land.pgOrNull(x+1,y),land.pgOrNull(x,y+1),land.pgOrNull(x-1,y)).filterNotNull() }
    val neardiag by Delegates.lazy { listOf(land.pgOrNull(x-1,y-1),land.pgOrNull(x+1,y+1),land.pgOrNull(x-1,y+1),land.pgOrNull(x+1,y-1)).filterNotNull() }
    val near8 by Delegates.lazy{
        ArrayList<Pg>().init{
            addAll(near)
            addAll(neardiag)
        }
    }

    fun invoke(tpPlace: TpPlace){
        this.tpPlace = tpPlace
    }

//    fun invoke(flat:TpFlat){
//        this.flat = flat
//    }
//
//    fun invoke(skil:TpSkil){
//        this.skil = skil
//    }
}



