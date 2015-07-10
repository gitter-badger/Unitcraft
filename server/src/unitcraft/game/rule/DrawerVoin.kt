package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import java.util.ArrayList
import java.util.HashMap

class DrawerVoin(r: Resource, drawer: Drawer, val objs: () -> Objs) {
    private val hintTileFlip = r.hintTileFlip
    private val hintTextLife = r.hintTextLife
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    private val tileHide = r.tile("hide")
    private val tlsVoins = HashMap<Kind, TlsVoin>()
    private val kinds = ArrayList<Kind>()
    val tileStts = ArrayList<(Voin) -> Int?>()

    init {
        drawer.onDraw(PriorDraw.voin) { side, ctx ->
            for (voin in objs().filterIsInstance<Voin>().byKind(kinds)) {
                val shape = voin.shape
                when (shape) {
                    is Singl -> {
                        ctx.drawTile(shape.pg, (tlsVoins[voin.kind])(side, voin), if (shape.flip) hintTileFlip else null)
                        ctx.drawText(shape.pg, voin.life.value, hintTextLife)
                        if (voin is VoinFuel) ctx.drawText(shape.pg, voin.fuel, hintTextEnergy)
                        if (voin.hided) ctx.drawTile(shape.pg, tileHide)
                        tileStts.forEach { it(voin)?.let { ctx.drawTile(shape.pg, it) } }
                    }
                    else -> throw Err("unknown shape=$shape")
                }
            }
        }
    }

    fun addKind(kind: Kind, tls: TlsVoin) {
        kinds.add(kind)
        tlsVoins[kind] = tls
    }

    fun addTileStt(tile: (Voin) -> Int?) {
        tileStts.add(tile)
    }
}

class EditorVoin(val editor: Editor, val hider: Hider, val lifer: Lifer, val objs: () -> Objs) {
    private val kinds = ArrayList<Kind>()
    private val tiles = ArrayList<Int>()

    fun build() {
        editor.onEdit(tiles, { pg, side, num ->
            val voin = Voin(kinds[num], Singl(ZetOrder.voin,pg), hider, lifer)
            voin.side = side
            (voin.shape as? Singl)?.let { it.flip = pg.x > pg.pgser.xr / 2 }
            objs().add(voin)
        }, { pg ->
            objs().byPg(pg).filterIsInstance<Voin>().firstOrNull()?.let {
                objs().remove(it)
            } ?: false
        })
    }

    fun addKind(kind: Kind, tile: Int) {
        kinds.add(kind)
        tiles.add(tile)
    }
}