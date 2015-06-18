package unitcraft.game

import org.json.simple.JSONAware
import java.util.ArrayList

class Traces : JSONAware{
    val traces = ArrayList<Trace>()

    fun add(trace:Trace){
        trace.exe()
        traces.add(trace)
    }

    fun rem(trace:Trace){
        trace.undo()
        traces.remove(trace)
    }

    fun clear(){
        traces.clear()
    }

    override fun toJSONString() = traces.toJSONString()
}

abstract class Trace : JSONAware{
    abstract fun exe()
    abstract fun undo()
    abstract fun dabsOnGrid():List<DabOnGrid>

    override fun toJSONString() = dabsOnGrid().toJSONString()
}