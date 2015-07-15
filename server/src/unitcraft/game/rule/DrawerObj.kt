package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class DrawerObj(r: Resource, drawer: Drawer,hider:Hider,sider:Sider, spoter: Spoter, val objs: () -> Objs) {
    private val hintTileFlip = r.hintTileFlip
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    private val tileHide = r.tile("hide")

    val tileStts = ArrayList<(Obj, Side) -> Tile?>()
    val draws = ArrayList<(Obj, Side,CtxDraw) -> Unit>()

    init {
        drawer.onDraw(PriorDraw.voin) { side, ctx ->
            for (obj in objs()) {
                val shape = obj.shape
                ctx.drawTile(obj.head(), obj.<SelTlsObj>()()(side, sider.side(obj), obj.isFresh), if (obj.flip) hintTileFlip else null)
                if (hider.isHided(obj,side)) ctx.drawTile(shape.head, tileHide)
                draws.forEach{it(obj,side,ctx)}
                tileStts.forEach { it(obj,side)?.let { ctx.drawTile(shape.head, it) } }
            }
        }
    }

    fun tlsObj(obj:Obj,tls:()->TlsVoin){
        obj.data(SelTlsObj(tls))
    }

    class SelTlsObj(val tls:()->TlsVoin):Data()
}

class DrawerFlat(val drawer: Drawer,val sider:Sider, val objs: () -> Objs) {
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