package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.server.Side
import java.util.ArrayList

class Tracer(r:Resource){
//    val traces = ArrayList<Trace>()
//
//    fun add(trace:Trace){
//        traces.add(trace)
//    }
//
//    fun rem(trace:Trace){
//        traces.remove(trace)
//    }
//
//    fun clear(){
//        traces.clear()
//    }
    // сам подписывается на нужные события и собирает следы
//    fun move()
//    fun damage()
//    fun heal()
//    fun objCreated()

    fun traces(side: Side):List<DabOnGrid>{
        return emptyList()
    }
}

abstract class Trace : JSONAware{
    abstract fun dabsOnGrid():List<DabOnGrid>

    override fun toJSONString() = dabsOnGrid().toJSONString()
}