package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class PointControl(val r: Resource,val stager: Stager, val sider: Sider,val drawer: DrawerPointControl, val editor: EditorPointControl, val objs: () -> Objs) {
    val kinds = ArrayList<Kind>()
    val kindsCanCapture = ArrayList<Kind>()

    init{
        stager.onEndTurn {
            for (point in objs().byKind(kinds))
                for (voin in objs().byKind(kindsCanCapture))
                    if (point.shape.pgs.intersect(voin.shape.pgs).isNotEmpty())
                        sider.capture(voin, point)
        }
    }

    fun add(kind:Kind) {
        val tls = r.tlsObjSide(kind.name)
        kinds.add(kind)
        sider.kinds.add(kind)
        drawer.regKind(kind, tls)
        editor.addKind(kind, tls.neut)
    }
}

class Flag(val pointControl: PointControl) {
    init {
        pointControl.add(KindFlag)
    }
    private object KindFlag : Kind()
}

class Mine(val pointControl: PointControl) {
    init {
        pointControl.add(KindMine)
    }
    private object KindMine : Kind()
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

class DrawerPointControl(val drawer: Drawer,val sider:Sider, val objs: () -> Objs) {
    private val tlsPointControls = HashMap<Kind, TlsObjOwn>()
    private val kinds = ArrayList<Kind>()

    init {
        drawer.onDraw(PriorDraw.flat) { side, ctx ->
            for (obj in objs().byKind(tlsPointControls.keySet())) {
                val shape = obj.shape
                when (shape) {
                    is Singl -> {
                        ctx.drawTile(shape.head, tlsPointControls[obj.kind](side, sider.side(obj)))
                    }
                    else -> throw Err("unknown shape=$shape")
                }
            }
        }
    }

    fun regKind(kind: Kind, tlsObjOwn: TlsObjOwn) {
        kinds.add(kind)
        tlsPointControls[kind] = tlsObjOwn
    }
}

class EditorPointControl(val editor: Editor, val shaper: Shaper, val sider: Sider, val objs: () -> Objs) {

    private val kinds = ArrayList<Kind>()
    private val tiles = ArrayList<Int>()

    init {
        editor.onEdit(tiles, { pg, side, num ->
            val obj = shaper.create(kinds[num], Singl(ZetOrder.flat, pg))
            sider.change(obj, Side.n)
        }, { pg ->
            objs().byPg(pg).byKind(kinds).firstOrNull()?.let {
                objs().remove(it)
            } ?: false
        })
    }

    fun addKind(kind: Kind, tile: Int) {
        kinds.add(kind)
        tiles.add(tile)
    }
}
