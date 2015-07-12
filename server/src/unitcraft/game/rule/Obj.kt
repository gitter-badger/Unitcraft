package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.PriorDraw
import unitcraft.game.ZetOrder
import unitcraft.server.Side
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlin

open class Obj(var kind:Kind,var shape:Shape){
    val props = HashMap<String, Any>()
    val datas = HashMap<KClass<out Data>,Data>()

    fun get(key:String):Any? = props[key]
    fun getOrPut(key:String,def:()->Any) = props.getOrPut(key,def)

    fun contains(key:String):Boolean = props[key]!=null


    fun set(key:String,value:Any){ props[key] = value }
    fun remove(key:String) = props.remove(key)


    inline fun <reified T> invoke():T = props.values().first { it is T } as T

    inline fun <reified T:Data> data():T = datas[javaClass<T>().kotlin] as T

    override fun toString() = "Obj "+props

}

abstract class Data{
    open fun onEndTurn(){}
}

class Objs:MutableList<Obj> by ArrayList<Obj>(){
    var sideTurn: Side = Side.a
    val bonus = HashMap<Side, Int>()
    val point = HashMap<Side, Int>()
    var objAktLast: Obj? = null
}

fun <T:Obj> List<T>.byKind(kind:Kind) = filter { it.kind == kind }
fun <T:Obj> List<T>.byKind(kinds:Collection<Kind>) = filter { it.kind in kinds }
fun <T:Obj> List<T>.byPg(pg: Pg) = filter { pg in it.shape.pgs }
fun <T:Obj> List<T>.byZetOrder(zetOrder: ZetOrder) = filter { it.shape.zetOrder == zetOrder }
fun List<Obj>.lay(zetOrder: ZetOrder,pg:Pg) = firstOrNull { it.shape.zetOrder == zetOrder && pg in it.shape.pgs }

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