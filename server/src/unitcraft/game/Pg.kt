package unitcraft.game

import java.util.*

class Pg(val pgser: Pgser,val x:Int, val y:Int):Comparable<Pg>{

    val up by lazy(LazyThreadSafetyMode.NONE) { plus(Dr.up) }
    val rt by lazy(LazyThreadSafetyMode.NONE) { plus(Dr.rt) }
    val dw by lazy(LazyThreadSafetyMode.NONE) { plus(Dr.dw) }
    val lf by lazy(LazyThreadSafetyMode.NONE) { plus(Dr.lf) }
    val near by lazy(LazyThreadSafetyMode.NONE) { listOf(up, rt,pgser.pgOrNull(x,y+1),pgser.pgOrNull(x-1,y)).filterNotNull() }

    val all by lazy(LazyThreadSafetyMode.NONE) { pgser.pgs}

    override fun toString() = "$x $y"

    override fun compareTo(other: Pg) = x * pgser.yr + y - (other.x * pgser.yr + other.y)

    fun plus(dr:Dr) = pgser.pgOrNull(x+dr.x,y+dr.y)
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

enum class Dr(val x:Int,val y:Int){
    up(0,-1),rt(1,0),dw(0,1),lf(-1,0);

    operator fun unaryMinus() = values.first { it.x == -x && it.y == -y }
}