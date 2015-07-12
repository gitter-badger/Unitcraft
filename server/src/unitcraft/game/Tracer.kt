package unitcraft.game

import org.json.simple.JSONAware
import java.util.ArrayList

class Tracer : JSONAware{
    val traces = ArrayList<Trace>()

    fun add(trace:Trace){
        traces.add(trace)
    }

    fun rem(trace:Trace){
        traces.remove(trace)
    }

    fun clear(){
        traces.clear()
    }

    override fun toJSONString() = traces.toJSONString()
}

abstract class Trace : JSONAware{
    abstract fun dabsOnGrid():List<DabOnGrid>

    override fun toJSONString() = dabsOnGrid().toJSONString()
}