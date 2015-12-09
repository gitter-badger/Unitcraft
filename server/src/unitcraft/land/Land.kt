package unitcraft.land

import unitcraft.game.Pg
import unitcraft.game.Pgser
import unitcraft.game.rule.Flater
import unitcraft.game.rule.Objer
import unitcraft.inject.inject
import unitcraft.land.TpFlat.*
import unitcraft.land.TpSolid.builder
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Land(val mission: Int?) {
    val objer: Objer by inject()
    val flater: Flater by inject()

    val seed = mission?.toLong() ?: System.nanoTime()
    val r = Random(seed)

    val pgser = createPgser()
    val xr = pgser.xr
    val yr = pgser.yr

    val flats = ArrayList<Flat>()
    val solids = ArrayList<Solid>()
    val exc = ArrayList<Pg>()

    init {
        for (pg in pgser) {
            addFlat(pg, none, 0)
        }

        repeat(rndInt(0, 2)) { makeSpot(wild) }
        repeat(rndInt(0, 1)) { makeSpot(liquid) }

        var cur: List<Pg>

        if (r.nextBoolean()) {
            cur = Algs.river(this, exc)
            val idx = idxFlatRnd(liquid)
            cur.forEach {
                addFlat(it, liquid, idx)
            }
            exc.addAll(cur)
        }

        cur = Algs.spray(this, exc, rndInt(0, 5))
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

        val pgBaseA = pgRnd { it.isEdge() }
        solids.add(Solid(pgBaseA, builder, idxSolidRnd(builder), Side.a))
        val pgBaseB = pgRnd { it.isEdge() && it.distance(pgBaseA) >= 8 }
        solids.add(Solid(pgBaseB, builder, idxSolidRnd(builder), Side.b))
    }

    private fun makeSpot(tp:TpFlat) {
        var cur = Algs.spot(this, exc, rndInt(5, 15))
        var idx = idxFlatRnd(tp)
        cur.forEach {
            addFlat(it, tp, idx)
        }
        exc.addAll(cur)
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

    fun <E> selRnd(list: List<E>) = selRndOrNull(list)!!

    fun <E> selRndOrNull(list: List<E>) = if (!list.isEmpty()) list[r.nextInt(list.size)] else null

    fun idxFlatRnd(tpFlat: TpFlat) = r.nextInt(flater.maxFromTpFlat()[tpFlat]!!)

    fun idxSolidRnd(tpFlat: TpSolid) = r.nextInt(objer.maxFromTpSolid()[tpFlat]!!)

    private fun createPgser(): Pgser {
        val (x, y) = selRnd(dmns)
        return Pgser(x, y)
    }

    private fun rndInt(a: Int, b: Int) = a + r.nextInt(b + 1)

    companion object {
        val dmns = listOf(12 to 11, 12 to 10, 12 to 9, 11 to 11, 11 to 10, 11 to 9, 10 to 10, 10 to 9, 9 to 9)
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
        land.pgRndOrNull { it !in exc }?.let { lst.add(it) }
        while (lst.size < qnt) {
            land.selRndOrNull(lst.flatMap { it.near }.filterNot { it in exc || it in lst })?.let { lst.add(it) } ?: break
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

fun dist2(pg: Pg, pgOther: Pg) = Math.sqrt(Math.pow(pg.x.toDouble() - pgOther.x, 2.0) + Math.pow(pg.y.toDouble() - pgOther.y, 2.0))