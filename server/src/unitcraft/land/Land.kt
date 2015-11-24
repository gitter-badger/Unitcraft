package unitcraft.land

import unitcraft.game.Pg
import unitcraft.game.Pgser
import unitcraft.game.rule.Flater
import unitcraft.game.rule.Solider
import unitcraft.inject.inject
import unitcraft.land.TpFlat.*
import unitcraft.land.TpSolid.builder
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Land(val mission: Int?) {
    val solider: Solider by inject()
    val flater: Flater by inject()

    val seed = mission?.toLong() ?: System.nanoTime()
    val r = Random(seed)

    val pgser = createPgser()
    val xr = pgser.xr
    val yr = pgser.yr

    val flats = ArrayList<Flat>()
    val solids = ArrayList<Solid>()

    init {
        for (pg in pgser) {
            addFlat(pg, none, 0)
        }

        val exc = ArrayList<Pg>()
        var cur = Algs.spot(this, exc, 10)
        var idx = idxFlatRnd(wild)
        cur.forEach {
            addFlat(it, wild, idx)
        }
        exc.addAll(cur)

        cur = Algs.spot(this, exc, 10)
        idx = idxFlatRnd(wild)
        cur.forEach {
            addFlat(it, wild, idx)
        }
        exc.addAll(cur)

        cur = Algs.river(this, exc)
        idx = idxFlatRnd(liquid)
        cur.forEach {
            addFlat(it, liquid, idx)
        }
        exc.addAll(cur)

        cur = Algs.spray(this, exc, 1 + r.nextInt(5))
        cur.forEach { addFlat(it, special, idxFlatRnd(special)) }
        exc.addAll(cur)

        cur = Algs.spray(this, exc, 3)
        cur.forEach {
            addFlat(it, flag, 0, Side.a)
        }
        exc.addAll(cur)

        cur = Algs.spray(this, exc, 2)
        cur.forEach {
            addFlat(it, flag, 0, Side.b)
        }
        exc.addAll(cur)

        solids.add(Solid(pgser.pg(0, 3), builder, 0, Side.a))
        solids.add(Solid(pgser.pg(xr - 1, yr - 4), builder, 1, Side.b))
    }

    fun addFlat(pg: Pg, tpFlat: TpFlat, idx: Int, side: Side = sideRnd()) {
        flats.removeIf { it.pg == pg }
        flats.add(Flat(pg, tpFlat, idx, side))
    }

    fun sideRnd() = if (r.nextBoolean()) Side.a else Side.b

    fun pgRnd(predicate: (Pg) -> Boolean) = pgRndOrNull(predicate) ?: throw Err("predicate neverwhere true")

    fun pgRndOrNull(predicate: (Pg) -> Boolean): Pg? {
        val list = pgser.pgs.filter(predicate)
        return if (list.isEmpty()) null else selRnd(list)
    }

    fun pgRnd() = selRnd(pgser.pgs)

    fun <E> selRnd(list: List<E>) = if (!list.isEmpty()) list[r.nextInt(list.size)] else throw Err("list is empty")

    fun idxFlatRnd(tpFlat: TpFlat) = r.nextInt(flater.maxFromTpFlat()[tpFlat]!!)

    fun idxSolidRnd(tpFlat: TpSolid) = r.nextInt(solider.maxFromTpSolid()[tpFlat]!!)

    private fun createPgser(): Pgser {
        val (x, y) = selRnd(dmns)
        return Pgser(x, y)
    }

    companion object {
        val dmns = (9..12).flatMap{yr -> (yr..12).map{it to yr}}
    }
}

enum class TpFlat {
    none, solid, liquid, wild, special, flag
}

enum class TpSolid {
    std, builder
}

class Flat(val pg: Pg, val tpFlat: TpFlat, val num: Int, val side: Side) {

}

class Solid(val pg: Pg, val tpSolid: TpSolid, val num: Int, val side: Side) {

}

object Algs {
    fun spray(land: Land, exc: List<Pg>, qnt: Int): List<Pg> {
        val lst = ArrayList<Pg>()
        repeat(qnt) {
            lst.add(land.pgRnd { it !in exc })
        }
        return lst
    }

    fun spot(land: Land, exc: List<Pg>, qnt: Int): List<Pg> {
        val lst = ArrayList<Pg>()
        lst.add(land.pgRnd { it !in exc })
        repeat(qnt - 1) {
            lst.add(land.pgRnd { it !in exc && it !in lst && it.near.any { it in lst } })
        }
        return lst
    }

    fun river(land: Land, exc: List<Pg>): List<Pg> {
        val lst = ArrayList<Pg>()
        val start = land.pgRnd { it.isEdge() && it !in exc }
        val finish = land.pgRnd { it.isEdge() && it !in exc && it != start && dist2(it, start) > 5 }
        lst.addAll(land.pgser.pgs)
        lst.removeAll(exc)
        val pgsMust = ArrayList<Pg>()
        if (isConnected(start, finish, lst)) {
            while (true) {
                val next = land.pgRndOrNull { it != start && it != finish && it in lst && it !in pgsMust } ?: break
                if (isConnected(start, finish, lst - next)) {
                    lst.remove(next)
                } else {
                    pgsMust.add(next)
                }
            }
            return lst
        } else return emptyList()
    }

    private fun isConnected(start: Pg, finish: Pg, where: List<Pg>): Boolean {
        val wave = ArrayList<Pg>()
        val que = ArrayList<Pg>()
        que.add(start)
        wave.add(start)
        while (true) {
            if (que.isEmpty()) break
            val next = que.removeAt(0).near.filter { it in where && it !in wave }
            que.addAll(next)
            wave.addAll(next)
        }
        return finish in wave
    }
}

fun Pg.isCorner() = x == 0 && y == 0 || x == pgser.xr - 1 && y == 0 || x == pgser.xr - 1 && y == pgser.yr - 1 || x == 0 && y == pgser.yr - 1

fun Pg.isEdge() = x == 0 || x == pgser.xr - 1 || y == 0 || y == pgser.yr - 1

fun dist2(pg: Pg, pgOther: Pg) = Math.sqrt(Math.pow(pg.x.toDouble() - pgOther.x, 2.0) + Math.pow(pg.y.toDouble() - pgOther.y, 2.0))