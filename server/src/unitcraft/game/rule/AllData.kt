package unitcraft.game.rule

import unitcraft.game.Grid
import unitcraft.game.Pg
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.exclude
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
    override fun toString() = "Flat $shape $datas"
}

class Flats: ListHasShape<Flat>{
    override val list = ArrayList<Flat>()


    fun get(pg: Pg) = list.byPg(pg)!!
}

class Obj(shape: Shape):HasShape(shape) {
    var side = Side.n
    var isFresh = false
    var flip = false
    var life = 5
    override fun toString() = "Solid $shape $datas"
}

class Objs: ListHasShape<Obj> {
    override val list = ArrayList<Obj>()

    fun objsSide(side:Side) = list.objsSide(side)

    fun get(pg: Pg) = list.byPg(pg)
}

inline fun <reified T : Data,A:HasShape> List<A>.by() = filter { it.has<T>() }.map{it to it<T>()}
fun <H:HasShape> List<H>.byPg(pg: Pg) = firstOrNull() { pg in it.shape.pgs }
fun <H:HasShape> List<H>.byClash(shape: Shape) = filter{obj -> shape.pgs.any{it in obj.shape.pgs}}
fun List<Obj>.objsSide(side:Side) = filter{it.side==side}

interface ListHasShape<H:HasShape>:Iterable<H>{
    val list: ArrayList<H>

    fun add(obj: H) {
        if(byClash(obj.shape).isNotEmpty()) throw Err("obj=$obj clash")
        list.add(obj)
    }

    override fun iterator() = list.iterator()
    fun remove(obj: H) = list.remove(obj)

    inline final fun <reified T : Data> by() = list.by<T,H>()
    fun byClash(shape: Shape) = list.byClash(shape)
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

interface Data

open class HasData{
    val datas = ArrayList<Data>()

    fun data(data: Data) {
        datas.add(data)
    }

    inline fun <reified T : Data> has(): Boolean = datas.firstOrNull{it is T}!=null

    inline fun <reified T : Data> remove() = datas.exclude{it is T}

    inline fun <reified T : Data> invoke(): T = datas.first{it is T} as T

    inline fun <reified T : Data> get() = datas.filterIsInstance<T>()
}

open class HasShape(var shape:Shape):HasData(){
    fun head() = shape.head
    fun near() = shape.near()
    fun further() = shape.further
}