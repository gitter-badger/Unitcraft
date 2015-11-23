package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class SkilerMove(r: Resource){
    val mover by inject<Mover>()

    init{
        val tileAktMove = r.tlsAktMove
        val mover = injectValue<Mover>()
        val spoter = injectValue<Spoter>()
        spoter.listOnTire.add{obj ->
            if(obj.has<SkilMove>()) obj<SkilMove>().refuel()
        }
        spoter.addSkilByBuilder<SkilMove> {
            val data = obj<SkilMove>()
            if(data.fuel>0){
                val wave = Wave(obj,data.fuel)
                for(pg in wave.pgs) {
                    //val move = Move(obj, obj.shape.headTo(pg), sideVid)

                    akt(pg, tileAktMove) {
                        val path = wave.path(pg)
                        for((can,move) in path){
                            if(can()) {
                                data.fuel -= 1
                                if(mover.move(move)) break
                            } else break
                        }
                    }
                }
            }            
        }
    }

    inner class Wave(obj:Obj,fuel:Int){
        val pgs = ArrayList<Pg>()

        init{
            val que = ArrayList<Pg>()
            que.add(obj.head())
            while(true){
                if (que.isEmpty()) break
                val next = que.removeAt(0).near.filter {
                    val move = Move(obj, obj.shape.headTo(pg), sideVid)
                    mover.canMove(move) && it !in pgsExclude && it !in wave
                }
            }
        }

        fun path(pg:Pg):List<Pair<()->Boolean,Move>>{

        }
    }

    private fun wave(start: Pg, lifer: Lifer, pgsExclude:List<Pg>): List<Pg> {
        val wave = ArrayList<Pg>()
        val que = ArrayList<Pg>()
        que.add(start)
        wave.add(start)
        while (true) {
            if (que.isEmpty()) break
            val next = que.removeAt(0).near.filter { lifer.canDamage(it) && it !in pgsExclude && it !in wave }
            que.addAll(next)
            wave.addAll(next)
        }
        return wave
    }
}

class SkilMove(var fuelMax:Int = 3):Data{
    var fuel = fuelMax

    fun refuel(){
        fuel = fuelMax
    }
}

class SkilerHit(r:Resource){
    init{
        val tlsAkt = r.tileAkt("hit")
        val lifer = injectValue<Lifer>()
        val spoter =  injectValue<Spoter>()
        spoter.addSkil<DataHit>(){sideVid, obj ->
            val data = obj<DataHit>()
            obj.near().filter { lifer.canDamage(it) }.map {
                AktSimple(it, tlsAkt) {
                    lifer.damage(it,data.dmg)
                    spoter.tire(obj)
                }
            }
        }
    }
}

class DataHit(val dmg:Int) : Data
