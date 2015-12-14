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

    var qntTurnLeft = 10
    var sideFirst:Side? = null

    var sideTurn = Side.a

    var objAktLast: Obj? = null
    var objNeedTire: Obj? = null

    val traces = HashMap<Side, ArrayList<DabOnGrid>>().apply {
        Side.ab.map { put(it, ArrayList<DabOnGrid>()) }
    }
}

class Flat(pg: Pg) : HasPg(pg) {
    override fun toString() = "Flat $pg $datas"
}

class Flats : ListHasShape<Flat> {
    override val list = ArrayList<Flat>()

    fun add(flat: Flat) {
        if (byPg(flat.pg) != null) throw Err("flat=$flat clash")
        list.add(flat)
    }

    fun sort() = list.apply { Collections.sort(list, compareBy { it.pg }) }

    operator fun get(pg: Pg) = list.byPg(pg)!!

    inline fun <reified D : Data> by() = filter { it.has<D>() }

    inline fun <reified T : Data> bothBy() = filter { it.has<T>() }.map { it to it<T>() }
}

class Obj(pg: Pg) : HasPg(pg) {
    var side:Side? = null
    var isFresh = false
    var flip = pg.x > pg.pgser.xr / 2
    var life = 3
    var hide = false

    fun isVid(sideVid: Side) = side==null || side == sideVid || !hide

    fun sidesVid() = Side.ab.filter { isVid(it) }

    fun isEnemy(side: Side?) = if(side==null) false else this.side == side.vs

    fun isAlly(side: Side?) = if(side==null) false else this.side == side

    override fun toString() = "Solid $pg $datas"
}

class Objs : ListHasShape<Obj> {
    override val list = ArrayList<Obj>()

    fun sort(): List<Obj> = list.apply { list.sort(compareBy { it.pg }) }

    fun bySide(side: Side?) = list.bySide(side)

    inline fun <reified D : Data> by() = filter { it.has<D>() }
    inline fun <reified D : Data> by(side:Side?) = filter { it.side == side && it.has<D>() }

    inline fun <reified T : Data> bothBy() = filter { it.has<T>() }.map { it to it<T>() }

    inline fun <reified T : Data> bothBy(side:Side?) = filter { it.side == side && it.has<T>() }.map { it to it<T>() }

    operator fun get(pg: Pg) = list.byPg(pg)
}

fun <H : HasPg> List<H>.byPg(pg: Pg) = firstOrNull() { pg == it.pg }
fun List<Obj>.bySide(side: Side?) = filter { it.side == side }

interface ListHasShape<H : HasPg> : Iterable<H> {
    val list: ArrayList<H>

    override fun iterator() = list.iterator()
    fun remove(obj: H) = list.remove(obj)

    fun byPg(pg: Pg) = list.byPg(pg)

    fun replace(obj: H) {
        byPg(obj.pg)?.let { list.remove(obj) }
        list.add(obj)
    }
}



interface Data

open class HasData {
    val datas = ArrayList<Data>()

    inline fun <reified T : Data> add(data: T) {
        if (has<T>()) remove<T>()
        datas.add(data)
    }

    inline fun <reified T : Data> has(): Boolean = datas.firstOrNull { it is T } != null

    inline fun <reified T : Data> remove() = datas.exclude { it is T }

    inline operator fun <reified T : Data> invoke() = datas.first { it is T } as T

    inline fun <reified T : Data> orNull() = datas.firstOrNull { it is T } as T?

    inline fun <reified T : Data> orPut(v: () -> T) = if (has<T>()) invoke<T>() else v().apply { add(this) }

    inline fun <reified T : Data> data(noinline fn: (T) -> Unit){
        orNull<T>()?.let{ fn(this<T>()) }
    }
}

open class HasPg(var pg: Pg) : HasData() {
    fun near() = pg.near

    fun further() = pg.further

}


