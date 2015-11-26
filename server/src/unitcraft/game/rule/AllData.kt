package unitcraft.game.rule

import unitcraft.game.DabOnGrid
import unitcraft.game.Pg
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.exclude
import java.util.*

class AllData {
    var sideWin: Side? = null
    val objs = Objs()
    val flats = Flats()
    val corpses = Objs()

    val bonus = HashMap<Side, Int>()

    val point = HashMap<Side, Int>().apply {
        Side.ab.map { put(it, 15) }
    }

    var sideTurn: Side = Side.a

    var objAktLast: Obj? = null
    var objNeedTire: Obj? = null

    var needJoin = true

    val traces = HashMap<Side, ArrayList<DabOnGrid>>().apply {
        Side.ab.map { put(it, ArrayList<DabOnGrid>()) }
    }
}

class Flat(pg: Pg) : HasPg(pg) {
    override fun toString() = "Flat $pg $datas"
}

class Flats : ListHasShape<Flat> {
    override val list = ArrayList<Flat>()

    fun sort() = list.apply { Collections.sort(list, compareBy { it.pg }) }

    operator fun get(pg: Pg) = list.byPg(pg)!!
}

class Obj(pg: Pg) : HasPg(pg) {
    var side = Side.n
    var isFresh = false
    var flip = pg.x > pg.pgser.xr / 2
    var life = 5
    var hide = false

    fun isVid(sideVid: Side) = side.isN || side == sideVid || !hide

    override fun toString() = "Solid $pg $datas"
}

class Objs : ListHasShape<Obj> {
    override val list = ArrayList<Obj>()

    fun sort(): List<Obj> = list.apply { list.sort(compareBy { it.pg }) }

    fun bySide(side: Side) = list.bySide(side)

    operator fun get(pg: Pg) = list.byPg(pg)
}

inline fun <reified T : Data, A : HasPg> List<A>.by() = filter { it.has<T>() }.map { it to it<T>() }
fun <H : HasPg> List<H>.byPg(pg: Pg) = firstOrNull() { pg == it.pg }
fun List<Obj>.bySide(side: Side) = filter { it.side == side }

interface ListHasShape<H : HasPg> : Iterable<H> {
    val list: ArrayList<H>

    fun add(obj: H) {
        if (byPg(obj.pg) != null) throw Err("obj=$obj clash")
        list.add(obj)
    }

    override fun iterator() = list.iterator()
    fun remove(obj: H) = list.remove(obj)

    fun byPg(pg: Pg) = list.byPg(pg)

    fun replace(obj: H){
        byPg(obj.pg)?.let{list.remove(obj)}
        list.add(obj)
    }
}

inline fun <reified T : Data, H : HasPg> ListHasShape<H>.by() = list.by<T, H>()

interface Data

open class HasData {
    val datas = ArrayList<Data>()

    inline fun <reified T : Data> data(data: T) {
        if (has<T>()) throw Err("duplicate data on $this")
        datas.add(data)
    }

    inline fun <reified T : Data> has(): Boolean = datas.firstOrNull { it is T } != null

    inline fun <reified T : Data> remove() = datas.exclude { it is T }

    inline operator fun <reified T : Data> invoke(): T {
        val data = datas.firstOrNull { it is T }
        if (data == null) println(T::class)
        return data as T
    }

    inline fun <reified T : Data> get() = datas.filterIsInstance<T>()

    inline fun <reified T : Data> orPut(v: () -> T) = if (has<T>()) invoke<T>() else v().apply { data(v()) }
}

open class HasPg(var pg: Pg) : HasData() {
    fun near() = pg.near

    fun further() = pg.further

}


