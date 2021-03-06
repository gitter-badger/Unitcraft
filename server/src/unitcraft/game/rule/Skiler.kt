package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class SkilerMove(r: Resource) {
    val mover by inject<Mover>()

    init {
        val tileAktMove = r.tlsAktMove
        val mover = injectValue<Mover>()
        val spoter = injectValue<Spoter>()
        spoter.listOnTire.add { obj ->
            if (obj.has<SkilMove>()) obj<SkilMove>().refuel()
        }
        spoter.addSkilByBuilder<SkilMove> {
            val data = obj<SkilMove>()
            if (data.fuel > 0) {
                val wave = Wave(obj, data.fuel, sideVid)
                for (pg in wave.pgs()) {
                    //val move = Move(obj, obj.shape.headTo(pg), sideVid)
                    akt(pg, tileAktMove) {
                        val pgOld = obj.pg
                        val path = wave.path(pg)
                        mover.movePath(obj, path, sideVid)
                        data.fuel -= obj.pg.distance(pgOld)
                    }
                }
            }
        }
    }

    inner class Wave(obj: Obj, fuel: Int, sideVid: Side) {
        private val map = HashMap<Pg, Int>()

        init {
            val que = ArrayList<Pair<Pg, Int>>()
            que.add(obj.pg to 0)
            while (true) {
                if (que.isEmpty()) break
                val (cur, cost) = que.removeAt(0)
                if (cost == fuel) continue
                val next = cur.near.filter {
                    mover.isMove(obj, cur, it, sideVid) && it !in map && it != obj.pg
                }.map { it to cost + 1 }
                map.putAll(next)
                que.addAll(next)
            }
        }

        fun path(pgFinish: Pg): List<Pg> {
            if (pgFinish !in map) throw Err("pgFinish($pgFinish) is not in wave")
            val path = ArrayList<Pg>()
            path.add(pgFinish)
            var cur = pgFinish to map[pgFinish]!!
            while (true) {
                cur = cur.first.near.reversed().map { it to (map[it] ?: Int.MAX_VALUE) }.filter { it.second < cur.second }.minBy { it.second } ?: break
                path.add(cur.first)
            }
            return path.reversed()
        }

        fun pgs() = map.keys
    }

    fun slow(obj: Obj) {
        obj.orNull<SkilMove>()?.slow()
    }

    fun remove(obj: Obj) {
        obj.remove<SkilMove>()
    }

    fun spend(obj: Obj) {
        obj.orNull<SkilMove>()?.spend()
    }

    fun spendAll(obj: Obj) {
        obj.orNull<SkilMove>()?.spendAll()
    }

    fun fuel(obj: Obj) = obj<SkilMove>().fuel

}

class SkilMove(var fuelMax: Int = 3) : Data {
    var fuel = fuelMax

    fun refuel() {
        fuel = fuelMax
    }

    fun slow(){
        fuelMax -= 1
        if(fuel>fuelMax) fuel = fuelMax
    }

    fun spend(){
        fuel -= 1
    }

    fun spendAll(){
        fuel = 0
    }
}

class SkilerHit(r: Resource) {
    init {
        val tlsAkt = r.tileAkt("hit")
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkil<DataHit>() { sideVid, obj ->
            val data = obj<DataHit>()
            obj.near().filter { lifer.canDamage(it) }.map {
                AktSimple(it, tlsAkt) {
                    lifer.damage(it, data.dmg)
                    spoter.tire(obj)
                }
            }
        }
    }
}

class DataHit(val dmg: Int) : Data
