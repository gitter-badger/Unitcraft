package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class DrawerVoin(r: Resource, drawer: Drawer,hider:Hider,sider:Sider, spoter: Spoter, val objs: () -> Objs) {
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
                ctx.drawTile(shape.head, tlsVoins[obj.kind](side, sider.side(obj), spoter.isFresh(obj)), if (obj["flip"] as Boolean? == true) hintTileFlip else null)
                if (hider.isHided(obj,side)!=null) ctx.drawTile(shape.head, tileHide)
                draws.forEach{it(obj,side,ctx)}
                tileStts.forEach { it(obj,side)?.let { ctx.drawTile(shape.head, it) } }
            }
        }
    }
}