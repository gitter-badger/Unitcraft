package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class Electric(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd,OnRaise{
    override val tlsVoin = r.tlsVoin("electric")
    val hintTrace = r.hintTileTouch
    val tileTrace = r.tile("electric.akt")

    override fun focus() = grid().map{it.key to it.value.side}.toList()


    override fun raise(aim: Aim, pg: Pg, pgSrc: Pg, side: Side,r:Raise) {
        return
    }

//    fun wave(pgs: HashMap<Pg, List<Voin>>,que:ArrayList<Pg>) {
//        que.firstOrNull()?.let { pg ->
//            que.remove(0)
//            pgs[pg] = g.info(MsgVoin(pg)).all
//            que.addAll(pg.near.filter { it !in pgs && g.info(MsgVoin(it)).all.isNotEmpty() })
//            wave(pgs,que)
//        }
//    }
//
//    fun hitElectro(pgAim:Pg,pgFrom:Pg){
//        val pgs = LinkedHashMap<Pg, List<Voin>>()
//        pgs[pgFrom] = emptyList()
//        val que = ArrayList<Pg>()
//        que.add(pgAim)
//        wave(pgs,que)
//        pgs.remove(pgFrom)
//        pgs.forEach{ p -> p.value.forEach{ g.make(EfkDmg(p.key,it)) }}
//        g.traces.add(TraceElectric(pgs.map { it.key }))
//    }
    inner class TraceElectric(val pgs:List<Pg>):Trace(){
        override fun dabsOnGrid() =
                pgs.map { DabOnGrid(it,DabTile(tileTrace,hintTrace)) }

    }
}
