package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Err
import java.util.HashMap

class DrawerVoin(r: Resource, drawer: Drawer, val objs: () -> Objs) {
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    val tileHide = r.tile("hide")
    val tlsVoins = HashMap<Kind, TlsVoin>()

    init {
        drawer.regTile { obj, side -> if (obj is Voin) tlsVoins[obj.kind]?.invoke(side, obj.side) else null }
        drawer.onDrawObj { obj, side, ctx ->
            if (obj is Voin) {
                val pg = pgLife(obj)
                ctx.drawText(pg, obj.life.value, hintTextLife)
                ctx.drawText(pg, obj.energy, hintTextEnergy)
                if (obj.hided) ctx.drawTile(pg, tileHide)
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

    fun regKind(kind: Kind, tls: TlsVoin) {
        tlsVoins[kind] = tls
    }

    fun regTlsVoin(tls: (Voin) -> TlsVoin) {

    }

    fun regTileStt(tile: (Voin) -> Int) {

    }
}

class EditorVoin(val editor: Editor, val shaper: Shaper, val sider: Sider, val hider: Hider, val enforcer: Enforcer, val lifer: Lifer, val objs: () -> Objs) {

    fun regKindVoin(kind: Kind, tlsVoin: TlsVoin) {
        editor.onEdit(tlsVoin.neut, { pg, side ->
            val voin = Voin(kind, shaper, sider, hider, enforcer, lifer)
            voin.shape = Singl(pg)
            voin.side = side
            voin.flip = pg.x > pg.pgser.xr / 2
            objs().add(voin)
        }, { pg ->
            objs().byPg(pg).filterIsInstance<Voin>().firstOrNull()?.let {
                objs().remove(it)
            } ?: false
        })
    }
}