package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import java.util.*

class DrawerVoin(r: Resource, drawer: Drawer, val objs: () -> Objs) {
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    val tileHide = r.tile("hide")
    val tlsDflt = r.tlsVoin("enot")
    val tlsVoins = HashMap<Kind, TlsVoin>()
    val tileStts = ArrayList<(Voin) -> Int?>()

    init {
        drawer.onDraw(PriorDraw.voin) { side, ctx ->
            for (voin in objs().filterIsInstance<Voin>()) {
                val shape = voin.shape
                when(shape){
                    is Singl -> {
                        ctx.drawTile(shape.pg, (tlsVoins[voin.kind]?:tlsDflt)(side, voin.side))
                        ctx.drawText(shape.pg, voin.life.value, hintTextLife)
                        if (voin is VoinFuel) ctx.drawText(shape.pg, voin.fuel, hintTextEnergy)
                        if (voin.hided) ctx.drawTile(shape.pg, tileHide)
                        tileStts.forEach{ it(voin)?.let{ ctx.drawTile(shape.pg, it) } }
                    }
                    else -> throw Err("unknown shape=$shape")
                }
            }
        }
    }

    private fun pgLife(obj: Voin): Pg {
        val shape = obj.shape
        return when (shape) {
            is Singl -> shape.pg
            else -> throw Err("unknown shape=$shape")
        }
    }

    fun addTile(kind: Kind, tls: TlsVoin) {
        tlsVoins[kind] = tls
    }

    fun addTile(tls: (Voin) -> TlsVoin) {

    }

    fun addTileStt(tile: (Voin) -> Int?) {
        tileStts.add(tile)
    }
}

class EditorVoin(val editor: Editor, val hider: Hider, val enforcer: Enforcer, val lifer: Lifer, val objs: () -> Objs) {

    fun regKindVoin(kind: Kind, tlsVoin: TlsVoin) {
        editor.onEdit(tlsVoin.neut, { pg, side ->
            val voin = Voin(kind, Singl(pg),hider, enforcer, lifer)
            voin.side = side
            (voin.shape as? Singl)?.let{ it.flip = pg.x > pg.pgser.xr / 2}
            objs().add(voin)
        }, { pg ->
            objs().byPg(pg).filterIsInstance<Voin>().firstOrNull()?.let {
                objs().remove(it)
            } ?: false
        })
    }
}