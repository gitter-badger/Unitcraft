package unitcraft.lander

import unitcraft.game.Pg
import unitcraft.game.Pgser
import unitcraft.lander.TpObj.fabriker
import unitcraft.lander.TpObj.std
import unitcraft.server.Side
import java.util.*

class MoldLand(val fn: (Random, Pgser, Map<TpFlat, Int>, Map<TpObj, Int>) -> Land) {
    operator fun invoke(r: Random, p: Pgser, mf: Map<TpFlat, Int>, mo: Map<TpObj, Int>) = fn(r, p, mf, mo)
}

class Land(val random: Random, val pgser: Pgser, val maxTpFlat: Map<TpFlat, Int>, val maxTpObj: Map<TpObj, Int>) {
    val xr = pgser.xr
    val yr = pgser.yr

    val flats = HashMap<Pg, FlatLand>()
    val objs = HashMap<Pg, ObjLand>()

    val exc = HashSet<Pg>()

    fun rnd(range: IntRange) = rnd(range.toList())!!

    fun <E> rnd(list: List<E>) = if (list.isEmpty()) null else list[random.nextInt(list.size)]

    fun lay(moldPrimt: MoldPrimt, tp: TpFlat) {
        var idx = idxFlatRnd(tp)
        moldPrimt(random, pgser, exc).pgsLay().apply { exc.addAll(this) }.forEach {
            addFlat(it, tp, idx)
        }
    }

    fun squad(moldPrimt: MoldPrimt) {
        val ctx = moldPrimt(random, pgser, emptySet())
        val side = sideRnd()
        squadByPrimt(ctx, TpPrism.lay, side)
        squadByPrimt(ctx, TpPrism.aux, side.vs)
    }

    private fun squadByPrimt(primt: Primt, tpPrism: TpPrism, side: Side) {
        val pgFabr = primt.pgRnd { primt.map[it] == tpPrism }!!
        objs[pgFabr] = ObjLand(fabriker, idxObjRnd(fabriker), side)
        var i = 0
        while (i < 10) {
            val pg = primt.pgRnd { it!=pgFabr && it !in objs.keys && primt.map[it] == tpPrism } ?: break
            objs[pg] = ObjLand(std, idxObjRnd(std), side)
            i += 1
        }
    }

    fun layDistinct(moldPrimt: MoldPrimt, tp: TpFlat) {
        moldPrimt(random, pgser, exc).pgsLay().apply { exc.addAll(this) }.forEach {
            addFlat(it, tp, idxFlatRnd(tp))
        }
    }

    fun addFlat(pg: Pg, tpFlat: TpFlat, idx: Int, side: Side = sideRnd()) {
        flats[pg] = FlatLand(tpFlat, idx, side)
    }

    fun sideRnd() = rnd(Side.ab)!!

    fun idxFlatRnd(tpFlat: TpFlat) = random.nextInt(maxTpFlat[tpFlat]!!)

    fun idxObjRnd(tp: TpObj) = random.nextInt(maxTpObj[tp]!!)
}

fun land(fn: Land.() -> Unit) = MoldLand { r, pgser, maxTpFlat, maxTpObj ->
    val land = Land(r, pgser, maxTpFlat, maxTpObj)
    land.fn()
    land
}

enum class TpFlat() {
    none, liquid, wild, special, flag
}

enum class TpObj {
    std, fabriker
}

class FlatLand(val tpFlat: TpFlat, val num: Int, val side: Side) {

}

class ObjLand(val tpObj: TpObj, val num: Int, val side: Side) {

}