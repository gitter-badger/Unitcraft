package unitcraft.land

import java.util.ArrayList
import kotlin.properties.Delegates
import java.util.HashMap
import unitcraft.game.*
import unitcraft.game.rule.CdxCatapult
import unitcraft.game.rule.CdxEnforcer
import unitcraft.game.rule.CdxPlace
import unitcraft.game.rule.CdxStaziser
import unitcraft.server.*
import unitcraft.land.Random
import kotlin.reflect.jvm.java

class Land(val mission: Int?,val cdxes:List<Cdx>){
    val seed = mission?.toLong() ?: System.nanoTime()
    val r = Random(seed)
    val sideFirst = if(r.nextBoolean()) Side.a else Side.b
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
                pg(Place.grass)
            }
        }
    }


    val fixs2 = HashMap<Pg, Map<Place, Int>>().init{
        for(pg in pgs){
            val map = HashMap<Place, Int>()
            for(tp in Place.values()){
                map[tp] = r.nextInt(CdxPlace.sizeFix[tp]!!)
            }
            this[pg] = map
        }
    }

    init {
        Algs.prod(this)
    }

    fun pg(x: Int, y: Int) = pgs[x * yr + y]

    fun pgOrNull(x: Int, y: Int) = if (isIn(x, y)) pg(x, y) else null

    fun pg(pg:unitcraft.game.Pg) = pg(pg.x,pg.y)

    fun place(pg:unitcraft.game.Pg) = Place.valueOf(pg(pg.x,pg.y).place.name())
    fun fixs(pg:unitcraft.game.Pg) = fixs2[pg(pg.x,pg.y)]

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



}

class Pg(val land:Land,val x:Int,val y:Int){
    var place: Place = Place.grass
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

    fun invoke(place:Place){
        this.place = place
    }

//    fun invoke(flat:TpFlat){
//        this.flat = flat
//    }
//
//    fun invoke(skil:TpSkil){
//        this.skil = skil
//    }
}

//class CreatorGameUc(val cdxes:List<Cdx>,val canEdit:Boolean,mission:Int?=null):CreatorGame{
//    val land = Land(mission, cdxes)
//
//    override fun createGame()=Game(cdxes.map{it.rule(land)},land.pgser,canEdit)
//}


