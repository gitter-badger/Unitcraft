package unitcraft.land

import unitcraft.game.Pg
import java.util.ArrayList
import java.util.HashMap

class Disbn<A>(vararg elems: Pair<A, Int>) {
    private val ranges = ArrayList<Pair<A, ClosedRange<Int>>>().apply {
        var sum = 0
        for ((a, chance) in elems) {
            add(a to (sum..sum + chance - 1))
            sum += chance
        }
    }
    private val sum = elems.fold(0) { acc, elem -> acc + elem.component2() }

    fun rnd(r: Random): A {
        val v = r.nextInt(sum)
        return ranges.first { v in it.second }.first
    }
}

class Wave(val pgStart: Pg, val radius: Double? = null, val cost: (Pg) -> Double?) {
    private val costs = HashMap<Pg, Double>()
    private val pgsFrom = HashMap<Pg, Pg>()

    init {
        costs[pgStart] = 0.0
        val q = listOf(pgStart).toArrayList()
        while (!q.isEmpty()) {
            val pgCur = q.removeAt(0)
            for ((pg, costNew) in pgsLinked(pgCur)) {
                if (radius == null || costNew <= radius) {
                    costs[pg] = costNew
                    pgsFrom[pg] = pgCur
                    q.add(pg)
                }
            }
        }
    }

    private fun pgsLinked(pgSrc: Pg): List<Pair<Pg, Double>> {
        val list = ArrayList<Pair<Pg, Double>>()
        for (pg in pgSrc.near) {
            val c = cost(pg)
            if (c != null && costs[pg] == null) list.add(pg to costs[pgSrc]!! + c)
        }
        list.sort { p1, p2 -> p1.component2().compareTo(p2.component2()) }
        return list
    }

    fun get(pg: Pg) = costs[pg]

    fun pgs() = costs.keys.toList()

    // путь от начала волны pgStart до цели pgEnd
    fun path(pgEnd: Pg): List<Pg> {
        if (costs[pgEnd] == null) return emptyList()
        var pg = pgEnd
        val path = listOf(pgEnd).toArrayList()
        while (pg != pgStart) {
            pg = pgsFrom[pg]!!
            path.add(pg)
        }
        return path.reversed()
    }
}
