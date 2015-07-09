package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import java.util.*

class Flag(r: Resource,override val grid: () -> Grid<PointControl>) {
    val tlsFlatControl = r.tlsObjOwn("flag")
//    endTurn(100){
//        for((pg,flat) in flats)
//            g.info(MsgVoin(pg)).voin?.let{
//                flat.side = it.side!!
//            }
//    }
}

class Mine(r: Resource,override val grid: () -> Grid<PointControl>) {
    val tlsFlatControl = r.tlsObjOwn("mine")
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

class Hospital(r: Resource,override val grid: () -> Grid<PointControl>) {
    val tlsFlatControl = r.tlsObjOwn("hospital")
}

class DrawerObjOwn(val drawer:Drawer,val objs:()-> Objs) {
    val kinds = ArrayList<Kind>()

    init{
        drawer.onDraw(PriorDraw.flat){ side,ctx ->
            for (objOwn in objs().filterIsInstance<ObjOwn>())
                ctx.drawTile(pg,herd.tlsFlatControl(side,p.side))
        }
    }

    override val tilesEditAdd = herds.map{it.tlsFlatControl.neut}


    fun regKind(kind:Kind){
        kinds.add(kind)
    }
}

class EditorObjOwn(val editor:Editor){
    override fun editAdd(pg: Pg, side: Side,num:Int) {
        herds[num].grid()[pg] = PointControl()
    }

    override fun editRemove(pg: Pg): Boolean {
        herds.forEach { if (it.grid().remove(pg)) return true }
        return false
    }
}

class PointControl(val stager:Stager,val objs:()-> Objs){
    val kinds = ArrayList<Kind>()

    init{
        stager.onEndTurn {
            for (obj in objs().byKind(kinds).filterIsInstance<ObjOwn>()) {
                for(voin in objs().filterIsInstance<Voin>()){
                    if(intersect(obj.shape,voin.shape)){
                        obj.side = voin.side
                    }
                }
            }
        }
    }

    private fun intersect(shape: Shape, shapeOther: Shape) =
        if(shape is Singl && shapeOther is Singl) shape.pg == shapeOther.pg else false


    fun regKind(kind:Kind){
        kinds.add(kind)
    }

}
