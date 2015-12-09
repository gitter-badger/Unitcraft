package unitcraft.game

import unitcraft.server.lzy
import java.util.*

class Pg(val pgser: Pgser, val x: Int, val y: Int) : Comparable<Pg> {

    val up by lzy { plus(Dr.up) }
    val rt by lzy { plus(Dr.rt) }
    val dw by lzy { plus(Dr.dw) }
    val lf by lzy { plus(Dr.lf) }
    val near by lzy { listOf(up, dw, rt, lf).filterNotNull() }
    val further: List<Pg> by lzy { near + near.flatMap { it.near }.filter { it != this }.distinct() }

    val all by lzy { pgser.pgs }

    override fun toString() = "$x $y"

    override fun compareTo(other: Pg) = x * pgser.yr + y - (other.x * pgser.yr + other.y)

    fun plus(dr: Dr) = pgser.pgOrNull(x + dr.x, y + dr.y)

    fun isNear(pg: Pg) = distance(pg) == 1

    fun distance(pg:Pg) = Math.abs(pg.x - x) + Math.abs(pg.y - y)

    fun dr(pg: Pg) = Dr.values().first {it.x == Integer.signum(pg.x-x) && it.y == Integer.signum(pg.y-y)}

    fun isEdge() = x == 0 || x == pgser.xr - 1 || y == 0 || y == pgser.yr - 1

    fun ray(dr: Dr,sizeMax:Int?=null): List<Pg> {
        val list = ArrayList<Pg>()
        var cur = this
        while(true){
            cur = cur.plus(dr)?:break
            list.add(cur)
            if(sizeMax!=null && list.size>=sizeMax) break
        }
        return list
    }
}

class Pgser(val xr: Int, val yr: Int) : Sequence<Pg> {
    val pgs: List<Pg> = ArrayList<Pg>(xr * yr).apply {
        repeat(xr) { x ->
            repeat(yr) { y ->
                add(Pg(this@Pgser, x, y))
            }
        }
    }

    fun isIn(x: Int, y: Int) = x >= 0 && x < xr && y >= 0 && y < yr

    fun pgOrNull(x: Int, y: Int) = if (isIn(x, y)) pg(x, y) else null

    fun pg(x: Int, y: Int) = pgs[x * yr + y]

    override fun iterator() = pgs.iterator()
}

enum class Dr(val x: Int, val y: Int) {
    up(0, -1), rt(1, 0), dw(0, 1), lf(-1, 0);

    operator fun unaryMinus() = values().first { it.x == -x && it.y == -y }
}