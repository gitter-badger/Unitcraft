package unitcraft.game.rule

import sideAfterChange
import unitcraft.game.*
import unitcraft.server.Side

class PointControl {
    var side = Side.n
}

class Flag(r: Resource,override val grid: () -> Grid<PointControl>) : OnHerdPointControl{
    override val tlsFlatControl = r.tlsFlatControl("flag")
//    endTurn(100){
//        for((pg,flat) in flats)
//            g.info(MsgVoin(pg)).voin?.let{
//                flat.side = it.side!!
//            }
//    }
}

class Mine(r: Resource,override val grid: () -> Grid<PointControl>) : OnHerdPointControl{
    override val tlsFlatControl = r.tlsFlatControl("mine")
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

class Hospital(r: Resource,override val grid: () -> Grid<PointControl>) : OnHerdPointControl{
    override val tlsFlatControl = r.tlsFlatControl("hospital")
}

class DrawerPointControl(exts:List<Ext>): OnEdit,OnDraw {
    val herds = exts.filterIsInstance<OnHerdPointControl>()

    override val tilesEditAdd = herds.map{it.tlsFlatControl.neut}

    override fun editAdd(pg: Pg, side: Side,num:Int) {
        herds[num].grid()[pg] = PointControl()
    }

    override fun editRemove(pg: Pg): Boolean {
        herds.forEach { if (it.grid().remove(pg)) return true }
        return false
    }

    override fun editChange(pg: Pg, side: Side) {
        herds.forEach { it.grid()[pg]?.let { v -> v.side = sideAfterChange(v.side, side) } }
    }

    override val prior = OnDraw.Prior.flat

    override fun draw(side: Side, ctx: CtxDraw) {
        for(herd in herds) for ((pg, p) in herd.grid()) ctx.drawTile(pg,herd.tlsFlatControl(side,p.side))
    }
}

interface OnHerdPointControl:Ext{
    val tlsFlatControl: TlsFlatControl
    val grid: () -> Grid<PointControl>
}