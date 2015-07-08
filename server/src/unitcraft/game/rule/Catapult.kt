package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(r: Resource,val grid :() -> Grid<Catapult.ctplt>) : OnDraw, OnEdit {
    val name = "catapult"
    val tile = r.tile(name)
    val tlsAkt = r.tlsAkt(name)

    override val prior = OnDraw.Prior.flat

    override fun draw(side: Side, ctx: CtxDraw) {
        for ((pg, _) in grid()) ctx.drawTile(pg, tile)
    }

    override val tilesEditAdd = listOf(tile)

    override fun editAdd(pg: Pg, side: Side,num:Int) {
        grid()[pg] = ctplt
    }

    override fun editRemove(pg: Pg) = grid().remove(pg)

    object ctplt

    //        info<MsgSpot>(20) {
    //            if (pgSrc in flats) g.voin(pgSpot,side)?.let {
    //                val tggl = g.info(MsgTgglRaise(pgSpot, it))
    //                if(!tggl.isCanceled) {
    //                    val r = Raise(pgSpot, tggl.isOn)
    //                    for (pg in g.pgs) if(!g.stop(EfkMove(pgSpot, pg, it))) r.add(pg, tlsAkt, EfkMove(pgSpot, pg, it))
    //                    add(r)
    //                }
    //            }
    //        }
}