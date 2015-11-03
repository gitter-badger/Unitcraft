package unitcraft.land

import unitcraft.game.Pg
import unitcraft.game.Pgser
import unitcraft.game.rule.Flater
import unitcraft.game.rule.Singl
import unitcraft.game.rule.Solider
import unitcraft.inject.inject
import unitcraft.land.TpFlat.*
import unitcraft.land.TpSolid.*
import unitcraft.server.Err
import java.util.*

class Land(val mission: Int?) {
//    val solider: Solider by inject()
//    val flater: Flater by inject()

    val seed = mission?.toLong() ?: System.nanoTime()
    val r = Random(seed)
//    val mapFlat = flater.maxFromTpFlat().mapValues { r.nextInt(it.value) }
    val yr = 9 + r.nextInt(7)
    val xr = if (yr == 15) 15 else yr + r.nextInt(15 - yr)
    val pgser = Pgser(xr, yr)


    val flats = ArrayList<Flat>()
    val solids = ArrayList<Solid>()

    init {
        for (pg in pgser) {
            addFlat(pg, none, 0)
        }

        Algs.spot(this).forEach {
            addFlat(it, wild, 0)
        }

        Algs.spot(this).forEach {
            addFlat(it, wild, 1)
        }

        Algs.river(this).forEach {
            addFlat(it, liquid, 0)
        }

        Algs.spray(this).forEach {
            addFlat(it, flag, 0)
        }

        solids.add(Solid(pgser.pg(0, 3), builder, 0))
        solids.add(Solid(pgser.pg(xr - 1, yr - 4), builder, 1))
    }

    fun addFlat(pg: Pg, tpFlat: TpFlat, idx: Int) {
        val flat = flats.firstOrNull() { it.pg == pg }
        if (flat != null) {
            flat.tpFlat = tpFlat
            flat.num = idx
        } else flats.add(Flat(pg, tpFlat, idx))
    }


}

enum class TpFlat {
    none, solid, liquid, wild, special, flag
}

enum class TpSolid {
    std, builder
}

class Flat(val pg: Pg, var tpFlat: TpFlat, var num: Int) {
    val shape = Singl(pg)
}

class Solid(val pg: Pg, var tpSolid: TpSolid, var num: Int) {
    val shape = Singl(pg)
}

class PrmAlg(val land: Land, val pgs: MutableList<Pg>) {
    fun add(pg: Pg) {
        pgs.add(pg)
    }

    val xl = land.xr - 1
    val yl = land.yr - 1

    fun pgRnd() = selRnd(land.pgser.pgs)

    fun pgRnd(predicate: (Pg) -> Boolean): Pg {
        val list = land.pgser.pgs.filter(predicate)
        if (list.isEmpty()) throw Err("predicate neverwhere true")
        return selRnd(list)
    }

    fun dist2(pg:Pg,pgOther:Pg) = Math.sqrt(Math.pow(pg.x.toDouble()-pgOther.x,2.0)+Math.pow(pg.y.toDouble()-pgOther.y,2.0))

    fun selRnd<E>(list: List<E>) = if (!list.isEmpty()) list[land.r.nextInt(list.size)] else throw Err("list is empty")

    fun isEdge(pg: Pg) = pg.x == 0 || pg.x == xl || pg.y == 0 || pg.y == yl

    fun isCorner(pg: Pg) = pg.x == 0 && pg.y == 0 || pg.x == xl && pg.y == 0 || pg.x == xl && pg.y == yl || pg.x == 0 && pg.y == yl
}

fun createAlg(fn: PrmAlg.() -> Unit): (Land) -> List<Pg> {
    return { land: Land ->
        val lst = ArrayList<Pg>()
        val p = PrmAlg(land, lst)
        p.fn()
        lst
    }
}

object Algs {
    val spray = createAlg {
        repeat(5) {
            add(pgRnd())
        }
    }
    val spot = createAlg {
        add(pgRnd())
        repeat(5) {
            add(pgRnd { it !in pgs && it.near.any { it in pgs } })
        }
    }

    val river = createAlg {
        val start = pgRnd { isEdge(it) }
        val pivot = pgRnd { isEdge(it) && it != start }
        val end = pgRnd { isEdge(it) && it != start && it != pivot }
        add(start)
        fun buildPathTo(aim: Pg) {
            while (true) {
                add(pgs.last().near.minBy { dist2(it, aim) }!!)
                if (pgs.last() == aim) break
            }
        }
        buildPathTo(pivot)
        buildPathTo(end)
    }
}