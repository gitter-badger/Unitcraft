package unitcraft.land

import unitcraft.game.Place.*
import java.util.HashMap
import java.util.ArrayList
import unitcraft.server.init
import unitcraft.server.exclude
import unitcraft.land.OpenSimplexNoise
import unitcraft.land.Random

object Algs {
    fun prod(land: Land) {
        disbnTimes.rnd(land.r).times {
            land.(disbnAlg.rnd(land.r).f)()}
//            land.(disbnAlgLate.rnd(land.r).f)()
        blur(land)
    }

    fun blur(land: Land) {
        val bad = land.pgs.filterTo(ArrayList<Pg>()){pg -> pg.neardiag.any{pg.place==it.place} && pg.near.all{pg.place!=it.place}}
        bad.addAll(land.pgs.filter{pg -> pg.near8.filter{pg.place==it.place}.size()<2})
          bad.forEach{it(land.selRnd(it.near).place)}
//        bad.forEach{it(PlaceMount)}

    }

    val wide = Alg(2) {
        val start = pgRnd { isEdge(it)}
        val place = selRnd(listOf(water,forest,sand,hill))
        val river = ArrayList<Pg>()
        river.add(start)
        if(isCorner(start)){
            river.add(selRnd(start.near))
        }
        river.add(river[river.size()-1].near.first{!isEdge(it)})
        val wide = ArrayList<Pg>()
        while(!isEdge(river[river.size()-1])) {
            val last = river[river.size()-1]
            val nexts = last.near.filter{it !in river && it.near.filter{it in river}.size()==1 && it.neardiag.filter{it in river}.size()<=1 && (river.size()>6 || !isEdge(it))}
            if (nexts.isEmpty() || nexts.last().place==water){break}
            river.add(selRnd(nexts))
            3.times{wide.add(selRnd(selRnd(river).near))}
            //            wide.add(selRnd(selRnd(river).near))
        }
        for (pg in river) pg(place)
        wide.exclude{it.near8.filter{it in river || it in wide}.size()<4}
        for (pg in wide) pg(place)
    }

    val riverSnake = Alg(6) {
        val start = pgRnd { isEdge(it)}
        val list = ArrayList<Pg>()
        list.add(start)
        if(isCorner(start)){
            list.add(selRnd(start.near))
        }
        list.add(list[list.size()-1].near.first{!isEdge(it)})
        while(!isEdge(list[list.size()-1])) {
            val last = list[list.size()-1]
            val nexts = last.near.filter{it !in list && it.near.filter{it in list}.size()==1 && it.neardiag.filter{it in list}.size()<=1 && (list.size()>6 || !isEdge(it))}
            if (nexts.isEmpty() || nexts.last().place==water){break}
            list.add(selRnd(nexts))
        }
        for (pg in list) pg(water)
    }

    val road = Alg(4) {
        val start = pgRnd { isEdge(it)}
        val list = ArrayList<Pg>()
        list.add(start)
        if(isCorner(start)){
            list.add(selRnd(start.near))
        }
        list.add(list[list.size()-1].near.first{!isEdge(it)})
        while(!isEdge(list[list.size()-1])) {
            val last = list[list.size()-1]
            val nexts = last.near.filter{it !in list && it.near.filter{it in list}.size()==1 && it.neardiag.filter{it in list}.size()<=1 && (list.size()>6 || !isEdge(it))}
            if (nexts.isEmpty() || nexts.last().near.any{it.place==water}){break}
            list.add(selRnd(nexts))
        }
        for (pg in list) pg(grass)
    }

    val field = Alg(4) {
        val place = selRnd(listOf(grass,water,forest,sand,hill))
        pgs.filter{it.place==grass}.forEach{it(place)}
    }

    val spotMount = Alg(1) {
        val start = pgRnd()
        val area = 10+r.nextInt(15)
        ArrayList<Pg>().init{
            add(start)
            add(selRnd(start.near))
            area.times{ add(pgRnd { it !in this && it.near8.filter{it in this}.size()>1})}
            addAll(pgs.filter{it.near8.filter{it in this}.size()==7})
        }.forEach{it(mount)}
    }
    val riverMount = Alg(1) {
        val start = pgRnd { isEdge(it)}
        val list = ArrayList<Pg>()
        list.add(start)
        if(isCorner(start)){
            list.add(selRnd(start.near))
        }
        list.add(list[list.size()-1].near.first{!isEdge(it)})
        while(!isEdge(list[list.size()-1])) {
            val last = list[list.size()-1]
            val nexts = last.near.filter{it !in list && it.near.filter{it in list}.size()==1 && it.neardiag.filter{it in list}.size()<=1 && (list.size()>6 || !isEdge(it))}
            if (nexts.isEmpty()){break}
            list.add(selRnd(nexts))
        }
        val pass = selRnd(list)
        list.remove(pass)
        list.remove(selRnd(pass.near.filter{it in list}))
        for (pg in list) pg(mount)
    }

    val romb = Alg(4) {
        val start = pgRnd{it.place!=water}
        val place = selRnd(listOf(grass, forest,sand,hill))
        val area = 2+r.nextInt(3)
        val spot = ArrayList<Pg>().init{
            add(start)
            add(selRnd(start.near))
            for(i in 0..area-1){
                val nexts = this.flatMap{it.near.filter{it !in this}}
                if (nexts.isEmpty()){break}
                addAll(nexts)
            }
            addAll(pgs.filter{it.near8.filter{it in this}.size()==7})
        }
        spot.forEach{it(place)}
        val ring = spot.filter{it.near8.any{it !in spot} }
        ring.forEach{it(water)}
    }


    val waterFobSpot = Alg(10) {
        val start = pgRnd{it.place!=water}
        val place = selRnd(listOf(grass,forest,sand,hill))
        val area = 4+r.nextInt(25)
        ArrayList<Pg>().init{
            add(start)
            add(selRnd(start.near))
            for(i in 0..area-1){
                val nexts = this.last().near8.filter{ it !in this && it.place!=water && it.near8.filter{it in this}.size()>1}
                if (nexts.isEmpty()){break}
                add(selRnd(nexts))
            }
            addAll(pgs.filter{it.near8.filter{it in this}.size()==7})
        }.forEach{it(place)}
    }

    val edgeSpot = Alg(20) {
        val start = pgRnd { isEdge(it)}
        val place = selRnd(listOf(forest,sand,hill))
        val area = 4+r.nextInt(30)
        ArrayList<Pg>().init{
            add(start)
            add(selRnd(start.near))
            for(i in 0..area-1){
                val nexts = this.last().near8.filter{ it !in this && it.place!=water && it.near8.filter{it in this}.size()>1}
                if (nexts.isEmpty()){break}
                add(selRnd(nexts))
            }
            addAll(pgs.filter{it.near8.filter{it in this}.size()==7})
        }.forEach{it(place)}
    }

    val patchwork = Alg(0) {
        val start = pgRnd()
        val area = 4+r.nextInt(30)
        ArrayList<Pg>().init{
            add(start)
            add(selRnd(start.near))
            area.times{ add(pgRnd { it !in this && it.near8.filter{it in this}.size()>1})}
            addAll(pgs.filter{it.near8.filter{it in this}.size()==7})
        }.forEach{ pg ->
            //val plcs = Land.places.filter{it !in pg.near.map{it.place}}
            val plcs = listOf(grass,water,forest,sand,hill)
            pg(selRnd(plcs))
        }
    }

    val rect = Alg(0) {
        val a = pgRnd()
        val b = pgRnd { it != a }
        val place = mount
        for (xx in Math.min(a.x, b.x)..Math.max(a.x, b.x)) for (yy in Math.min(a.y, b.y)..Math.max(a.y, b.y)) pg(xx, yy)(place)
    }

    val lakeSimplex = Alg(1) {
        val start = pgRnd()
        val noise = OpenSimplexNoise(r.seed())
        val ft = 3
        //        for(pg in land.pgs) if(noise.get(pg.x,pg.y,ft)>0.6) pg(PlaceSand)

        val wave = Wave(start, 1.0 + r.nextDouble(), { pg -> noise.get(pg.x, pg.y, ft) })
        for (pg in wave.pgs()) pg(water)
    }

    val riverWaveNoise = Alg(1) {
        val start = pgRnd { isEdge(it) }
        val end = pgRnd { isEdge(it) && if (start.x == 0 || start.x == xl) it.x != start.x else it.y != start.y }
        val noise = Noise(this)
        val wave = Wave(start) { pg -> Math.pow(noise[pg] * 10, 3.0) }
        for (pg in wave.path(end)) pg(water)
    }

    val lakeWaveNoise = Alg(1) {
        val start = pgRnd()
        val noise = Noise(this)
        val wave = Wave(start, 1.0) { noise[it] }
        for (pg in wave.pgs()) pg(water)
    }

    private val disbnTimes = Disbn(
            1 to 2,
            2 to 1,
            3 to 1,
            4 to 25,
            5 to 40,
            6 to 25,
            7 to 20,
            8 to 5,
            9 to 3,
            10 to 1
    )
    private val all = listOf(
            field,
            riverSnake,
            waterFobSpot,
            romb,
            patchwork,
            wide,
            road,
            edgeSpot
    )
    private val allLate = listOf(
            spotMount,
            riverMount
    )

    private val disbnAlg = Disbn(*all.map { it to it.chance }.copyToArray())
    private val disbnAlgLate = Disbn(*allLate.map { it to it.chance }.copyToArray())
}

class Alg(val chance: Int, val f: Land.() -> Unit)

class Disbn<A>(vararg elems: Pair<A, Int>) {
    private val ranges = ArrayList<Pair<A, Range<Int>>>().init {
        var sum = 0
        for ((a, chance) in elems) {
            add(a to (sum..sum + chance - 1))
            sum+=chance
        }
    }
    private val sum = elems.fold(0) {acc, elem -> acc + elem.component2() }

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
            val pgCur = q.remove(0)
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
            if (c != null && costs[pg] == null) list.add(pg to costs[pgSrc] + c)
        }
        list.sort { p1, p2 -> p1.component2().compareTo(p2.component2()) }
        return list
    }

    fun get(pg: Pg) = costs[pg]

    fun pgs() = costs.keySet().toList()

    // путь от начала волны pgStart до цели pgEnd
    fun path(pgEnd: Pg): List<Pg> {
        if (costs[pgEnd] == null) return listOf<Pg>()
        var pg = pgEnd
        val path = listOf(pgEnd).toArrayList()
        while (pg != pgStart) {
            pg = pgsFrom[pg]
            path.add(pg)
        }
        return path.reverse()
    }
}
