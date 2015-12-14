package unitcraft.lander

import unitcraft.game.Pg
import unitcraft.game.Pgser
import unitcraft.server.lzy
import java.util.*

enum class TpPrism {
    exc, lay, aux, three
}

class MoldPrimt(val fn: (Random, Pgser, Set<Pg>) -> Primt) {
    operator fun invoke(r: Random, p: Pgser, exc: Set<Pg>) = fn(r, p, exc)
}

//TODO exc превратить в предикат
class Primt(val random: Random, val pgser: Pgser, exc: Set<Pg>) {

    val map = HashMap<Pg, TpPrism>().apply {
        exc.forEach { this[it] = TpPrism.exc }
    }

    val xr = pgser.xr
    val yr = pgser.yr
    val xl = xr - 1
    val yl = yr - 1

    fun <E> rnd(list: List<E>) = if (list.isEmpty()) null else list[random.nextInt(list.size)]

    fun ppp() = pgser.pgs.filterNot { isExc(it) }
    fun ppp(cond: (Pg) -> Boolean) = ppp().filter { cond(it) }

    fun isFree(pg: Pg) = pg !in map
    fun isExc(pg: Pg) = map[pg] == TpPrism.exc

    fun isLay(pg: Pg) = map[pg] == TpPrism.lay

    fun isAux(pg: Pg) = map[pg] == TpPrism.aux

    fun lay(pg: Pg) {
        if (!isExc(pg)) map[pg] = TpPrism.lay
    }

    fun lay(pgs: List<Pg>) {
        pgs.forEach { lay(it) }
    }

    fun aux(pg: Pg) {
        if (!isExc(pg)) map[pg] = TpPrism.aux
    }

    fun aux(pgs: List<Pg>) {
        pgs.forEach { aux(it) }
    }

    fun pgsExc() = pgser.pgs.filter { isExc(it) }
    fun pgsLay() = pgser.pgs.filter { isLay(it) }
    fun pgsAux() = pgser.pgs.filter { isAux(it) }


    fun unlay(pg: Pg) {
        if (!isExc(pg)) map.remove(pg)
    }

    fun pgRnd() = rnd(ppp())
    fun pgRnd(cond: (Pg) -> Boolean) = rnd(ppp().filter { cond(it) })
    fun rndBln() = random.nextBoolean()
}

fun primt(fn: Primt.() -> Unit) = MoldPrimt { r, pgser, exc ->
    val prism = Primt(r, pgser, exc)
    prism.fn()
    prism
}

class Rect(val pg: Pg, val pgEnd:Pg){
    val pgs by lzy {
        (pg.x..pgEnd.x).flatMap { x -> (pg.y..pgEnd.y).map { y -> pg.pgser.pg(x, y) } }
    }
}
