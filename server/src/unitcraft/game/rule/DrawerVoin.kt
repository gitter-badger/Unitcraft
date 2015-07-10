package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import java.util.HashMap

class DrawerVoin(r: Resource, drawer: Drawer,hider:Hider, val objs: () -> Objs) {
    private val hintTileFlip = r.hintTileFlip
    private val hintTextLife = r.hintTextLife
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    private val tileHide = r.tile("hide")
    private val tlsVoins = HashMap<Kind, TlsVoin>()
    private val kinds = ArrayList<Kind>()
    val tileStts = ArrayList<(Obj, Side) -> Int?>()

    init {
        drawer.onDraw(PriorDraw.voin) { side, ctx ->
            for (voin in objs().byKind(kinds)) {
                val shape = voin.shape
                ctx.drawTile(shape.head, (tlsVoins[voin.kind])(side, voin), if (voin["flip"]!=null) hintTileFlip else null)
                if (hider.isHided(voin,side)!=null) ctx.drawTile(shape.head, tileHide)
                tileStts.forEach { it(voin,side)?.let { ctx.drawTile(shape.head, it) } }
            }
        }
    }

    fun addKind(kind: Kind, tls: TlsVoin) {
        kinds.add(kind)
        tlsVoins[kind] = tls
    }
}

class EditorVoin(val editor: Editor, val shaper: Shaper, val objs: () -> Objs) {
    private val kinds = ArrayList<Kind>()
    private val tiles = ArrayList<Int>()

    fun build() {
        editor.onEdit(tiles, { pg, side, num ->
//            val voin = Voin(kinds[num], Singl(ZetOrder.voin,pg))
//            voin.side = side
//            (voin.shape as? Singl)?.let { it.flip = pg.x > pg.pgser.xr / 2 }
//            objs().add(voin)
            val obj = shaper.create(kinds[num],Singl(ZetOrder.voin,pg))
            obj["flip"] = pg.x > pg.pgser.xr / 2
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