package unitcraft.game

import java.util.*

class Pg(val pgser: Pgser,val x:Int, val y:Int):Comparable<Pg>{

    val up by lazy(LazyThreadSafetyMode.NONE) { pgser.pgOrNull(x,y-1) }
    val rt by lazy(LazyThreadSafetyMode.NONE) { pgser.pgOrNull(x+1,y) }
    val dw by lazy(LazyThreadSafetyMode.NONE) { pgser.pgOrNull(x,y+1) }
    val lf by lazy(LazyThreadSafetyMode.NONE) { pgser.pgOrNull(x-1,y) }
    val near by lazy(LazyThreadSafetyMode.NONE) { listOf(up, rt,pgser.pgOrNull(x,y+1),pgser.pgOrNull(x-1,y)).filterNotNull() }

    val all by lazy(LazyThreadSafetyMode.NONE) { pgser.pgs}

    override fun toString() = "$x $y"

    override fun compareTo(other: Pg) = x * pgser.yr + y - (other.x * pgser.yr + other.y)
}

class Pgser(val xr:Int,val yr:Int):Sequence<Pg>{
    val pgs:List<Pg> = ArrayList<Pg>(xr*yr).apply{
        repeat(xr) {x -> repeat(yr) {y ->
            add(Pg(this@Pgser, x, y))
        }}
    }

    fun isIn(x: Int, y: Int) = x >= 0 && x < xr && y >= 0 && y < yr

    fun pgOrNull(x: Int, y: Int) = if (isIn(x, y)) pg(x, y) else null

    fun pg(x: Int, y: Int) = pgs[x * yr + y]

    override fun iterator() = pgs.iterator()
}

// TODO превратить его в ArrayList
class Grid<V:Any>:MutableMap<Pg, V> by HashMap<Pg,V>(){

//    fun get(pg:Pg) = objs[pg]
//
//    fun set(pg:Pg,value:V){ objs[pg] = value }
//
//    fun move(pgFrom:Pg,pgTo:Pg):V{
//        objs[pgTo] = objs.remove(pgFrom)!!
//        return objs[pgTo]
//    }
//
//    fun remove(pg: Pg) = objs.remove(pg)!=null
//
//    fun contains(pg:Pg) = objs.contains(pg)

//    override fun iterator() = objs.iterator()
}