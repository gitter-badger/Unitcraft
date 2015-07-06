package unitcraft.game.rule

import sideAfterChange
import unitcraft.game.*
import unitcraft.server.Side

class PointControl {
    var side = Side.n
}

class Flag(r: Resource, grid: () -> Grid<PointControl>) : OnEditDraw by HelpOnEditDrawPointControl(r, "flag", grid){
//    endTurn(100){
//        for((pg,flat) in flats)
//            g.info(MsgVoin(pg)).voin?.let{
//                flat.side = it.side!!
//            }
//    }
}

class Mine(r: Resource, grid: () -> Grid<PointControl>) : OnEditDraw by HelpOnEditDrawPointControl(r, "mine", grid){
    //        endTurn(100){
    //            for((pg,flat) in flats)
    //                g.info(MsgVoin(pg)).voin?.let{
    //                    flat.side = it.side!!
    //                }
    //            for((pg,flat) in flats) flat.side?.let{
    //                if(it == g.sideTurn) g.make(EfkGold(1,it))
    //            }
    //        }
}

class Hospital(r: Resource, grid: () -> Grid<PointControl>) : OnEditDraw by HelpOnEditDrawPointControl(r, "hospital", grid)

class HelpOnEditDrawPointControl(r:Resource,name:String,val grid: () -> Grid<PointControl>): OnEditDraw {
    val tls = r.tlsFlatControl(name)

    override val tileEditAdd = tls.neut

    override fun editAdd(pg: Pg, side: Side) {
        grid()[pg] = PointControl()
    }

    override fun editRemove(pg: Pg) = grid().remove(pg)

    override fun editChange(pg: Pg, side: Side) {
        grid()[pg]?.let { it.side = sideAfterChange(it.side,side) }
    }

    override val prior = OnDraw.Prior.flat

    override fun draw(side: Side, ctx: CtxDraw) {
        for ((pg, p) in grid()) ctx.drawTile(pg,tls(side,p.side))
    }
}

interface OnEditDraw: OnDraw, OnEdit