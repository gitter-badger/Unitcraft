package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.PriorDraw
import unitcraft.game.ZetOrder
import unitcraft.server.Side
import java.util.*

class Obj(var kind:Kind,var shape:Shape){
    val props = HashMap<String, Any>()

    fun get(key:String):Any? = props[key]
    fun set(key:String,value:Any){ props[key] = value }
    fun remove(key:String) = props.remove(key)
    fun getOrPut(key:String,def:()->Any) = props.getOrPut(key,def)

    inline fun <reified T> invoke():T = props.values().first { it is T } as T

    override fun toString(): String {
        return "Obj "+props
    }
}

class Objs:MutableList<Obj> by ArrayList<Obj>(){
}

fun <T:Obj> List<T>.byKind(kind:Kind) = filter { it.kind == kind }
fun <T:Obj> List<T>.byKind(kinds:Collection<Kind>) = filter { it.kind in kinds }
fun <T:Obj> List<T>.byPg(pg: Pg) = filter { pg in it.shape.pgs }
fun <T:Obj> List<T>.byZetOrder(zetOrder: ZetOrder) = filter { it.shape.zetOrder == zetOrder }

abstract class Shape(val zetOrder: ZetOrder,val head:Pg){
    abstract val pgs:List<Pg>
    abstract fun headTo(pgTo:Pg):Shape
}

data class Singl(zetOrder: ZetOrder,head:Pg) : Shape(zetOrder,head){
    override val pgs = listOf(head)
    override fun headTo(pgTo: Pg) = Singl(zetOrder,pgTo)
}

data class Quadr(zetOrder: ZetOrder,head:Pg) : Shape(zetOrder,head){
    override val pgs = listOf(head,head.rt,head.dw,head.rt?.dw).requireNoNulls()
    override fun headTo(pgTo: Pg) = Quadr(zetOrder,pgTo)
}

abstract class Kind{
    val name = this.javaClass.getSimpleName().substring(4).decapitalize()
}