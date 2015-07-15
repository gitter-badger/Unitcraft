package unitcraft.game.rule

import unitcraft.game.Grid
import unitcraft.game.Pg
import unitcraft.game.TpPlace
import unitcraft.game.ZetOrder
import unitcraft.server.Side
import unitcraft.server.init
import java.util.ArrayList
import java.util.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlin
import kotlin.properties.Delegates

class AllData{
    val objs = Objs()
    val flats = Flats()

    val bonus = HashMap<Side, Int>()

    val point = HashMap<Side, Int>().init{
        put(Side.a,15)
        put(Side.b,15)
    }

    var sideTurn: Side = Side.a

    var objAktLast: Obj? = null
}

class Flat(shape: Shape):HasShape(shape){
    var tpPlace = TpPlace.grass
    var fix:Map<TpPlace, Int> by Delegates.notNull()
}

class Flats: ListHasShape<Flat>{
    override val list = ArrayList<Flat>()


    fun get(pg: Pg) = list.byPg(pg)!!
}

fun List<Flat>.byPg(pg: Pg) = firstOrNull() { pg in it.shape.pgs }

class Obj(shape: Shape):HasShape(shape) {
    override fun toString() = "Solid $shape $datas"
}

class Objs: ListHasShape<Obj> {
    override val list = ArrayList<Obj>()

    fun get(pg: Pg) = list.byPg(pg)
}

inline fun <reified T : Data,A:HasShape> List<A>.by() = filter { javaClass<T>().kotlin in it.datas }.map{it to it<T>()}
fun List<Obj>.byPg(pg: Pg) = firstOrNull() { pg in it.shape.pgs }

interface ListHasShape<H :HasShape>{
    val list: ArrayList<H>

    fun add(obj: H) {
        list.add(obj)
    }

    fun iterator() = list.iterator()
    fun remove(obj: Obj) = list.remove(obj)

    inline final fun <reified T : Data> by() = list.by<T,H>()
}

abstract class Shape(val head: Pg) {
    abstract val pgs: List<Pg>
    abstract fun headTo(pgTo: Pg): Shape
    abstract fun near(): List<Pg>
    val further:List<Pg> by Delegates.lazy{ near().flatMap{it.near}.distinct().filter { it != head } }
}

data class Singl(head: Pg) : Shape(head) {
    override val pgs = listOf(head)
    override fun headTo(pgTo: Pg) = Singl(pgTo)
    override fun near() = head.near
}

data class Quadr(head: Pg) : Shape(head) {
    override val pgs = listOf(head, head.rt, head.dw, head.rt?.dw).requireNoNulls()
    override fun headTo(pgTo: Pg) = Quadr(pgTo)
    override fun near() = listOf(head.up, head.lf, head.dw?.lf, head.dw?.dw, head.rt?.up, head.rt?.rt).filterNotNull()
}

abstract class Kind {
    val name = this.javaClass.getSimpleName().substring(4).decapitalize()
}

abstract class Data

open class HasData{
    val datas = HashMap<KClass<out Data>, Data>()

    fun data(data: Data) {
        datas[data.javaClass.kotlin] = data
    }

    inline fun <reified T : Data> has(): Boolean = datas[javaClass<T>().kotlin] != null

    inline fun <reified T : Data> remove() = datas.remove(javaClass<T>().kotlin)

    inline fun <reified T : Data> invoke(): T = datas[javaClass<T>().kotlin] as T
}

open class HasShape(var shape:Shape):HasData(){
    fun head() = shape.head
    fun near() = shape.near()
    fun further() = shape.further
}