package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class PointControl(val r: Resource,val stager: Stager, val sider: Sider,val drawer: DrawerFlat, val shaper: Shaper, val objs: () -> Objs) {

    val tilesEditor = ArrayList<Tile>()
    val refinesEditor = ArrayList<(Flat,Pg,Side)->Unit>()

    init{
        stager.onEndTurn {
            for ((flat,point) in objs().by<Point>())
                for (voin in objs())
                    if (flat.shape.pgs.intersect(voin.shape.pgs).isNotEmpty())
                        point.side = voin.side
        }
    }

    fun add(obj:Obj,name:String) {
        val tls = r.tlsObjSide(name)
        val p= Point(tls)
        obj.data(p)
        drawer.tlsFlat(obj, {side -> p.tls(p.side,side)})
        shaper.addToEditor(kind,ZetOrder.flat, tls.neut)
    }

    class Point(val tls:TlsObjOwn):Data(){
        var side = Side.n
    }
}

class Flag(val pointControl: PointControl) {
    init {
        pointControl.add(KindFlag)
    }
    private object KindFlag : Kind()
}

class Mine(val pointControl: PointControl,val stager: Stager,val sider:Sider,val builder:Builder, val objs: () -> Objs) {
    init {
        stager.onEndTurn {
            val gold = objs().by<Mine>().filter{it.first.side == stager.sideTurn()}.size()
            builder.plusGold(stager.sideTurn(),gold)
        }
    }
    private object Mine : Data()
    //        endTurn(100){
    //            for((pg,flat) in flats) flat.side?.let{
    //                if(it == g.sideTurn) g.make(EfkGold(1,it))
    //            }
    //        }
}

class Hospital( val pointControl: PointControl) {
    init {
        pointControl.add(KindHospital)
    }
    private object KindHospital : Kind()
}



//class EditorPointControl(val editor: Editor, val shaper: Shaper, val sider: Sider, val objs: () -> Objs) {
//
//    private val kinds = ArrayList<Kind>()
//    private val tiles = ArrayList<Int>()
//
//    init {
////        editor.onEdit(tiles, { pg, side, num ->
////            shaper.create(kinds[num], Singl(ZetOrder.flat, pg))
////        }, { pg ->
////            objs().byPg(pg).byKind(kinds).firstOrNull()?.let {
////                objs().remove(it)
////            } ?: false
////        })
//    }
//
//    fun addKind(kind: Kind, tile: Int) {
//        kinds.add(kind)
//        tiles.add(tile)
//    }
//}
