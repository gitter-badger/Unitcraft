package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class DrawerVoin(r: Resource, drawer: Drawer,hider:Hider,sider:Sider, val objs: () -> Objs) {
    private val hintTileFlip = r.hintTileFlip
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    private val tileHide = r.tile("hide")

    val tlsVoins = HashMap<Kind, TlsVoin>()
    val tileStts = ArrayList<(Obj, Side) -> Int?>()
    val draws = ArrayList<(Obj, Side,CtxDraw) -> Unit>()

    init {
        drawer.onDraw(PriorDraw.voin) { side, ctx ->
            for (obj in objs().byKind(tlsVoins.keySet())) {
                val shape = obj.shape
                ctx.drawTile(shape.head, (tlsVoins[obj.kind])(side, sider.side(obj)), if (obj["flip"] as Boolean? == true) hintTileFlip else null)
                if (hider.isHided(obj,side)!=null) ctx.drawTile(shape.head, tileHide)
                draws.forEach{it(obj,side,ctx)}
                tileStts.forEach { it(obj,side)?.let { ctx.drawTile(shape.head, it) } }
            }
        }
    }
}

class EditorVoin(val editor: Editor, val shaper: Shaper,val sider: Sider, val objs: () -> Objs) {
    private val kinds = ArrayList<Kind>()
    private val tiles = ArrayList<Int>()

    init {
        editor.onEdit(tiles, { pg, side, num ->
//            val voin = Voin(kinds[num], Singl(ZetOrder.voin,pg))
//            voin.side = side
//            (voin.shape as? Singl)?.let { it.flip = pg.x > pg.pgser.xr / 2 }
//            objs().add(voin)
            val obj = shaper.create(kinds[num],Singl(ZetOrder.voin,pg))
            obj["flip"] = pg.x > pg.pgser.xr / 2
            sider.change(obj,side)
        }, { pg ->
            objs().byPg(pg).byKind(kinds).firstOrNull()?.let {
                shaper.remove(it)
            } ?: false
        })
    }

    fun addKind(kind: Kind, tile: Int) {
        kinds.add(kind)
        tiles.add(tile)
    }
}