package unitcraft.game.rule

import sideAfterChange
import unitcraft.game.*
import unitcraft.server.Side

class VoinSimple(val life: Life, var side: Side, var flip: Boolean) {
    var isHided: Boolean = false
    var energy = 3
}

class DrawerVoin(r: Resource, exts: List<Ext>) : OnDraw, OnEdit {
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    val tileHide = r.tile("hide")

    val onHerds = exts.filterIsInstance<OnHerd>()

    override val prior = OnDraw.Prior.voin

    override fun draw(side: Side, ctx: CtxDraw) {
        for (herd in onHerds) {
            for ((pg, v) in herd.grid()) {
                ctx.drawTile(pg, herd.tlsVoin(side, v.side), if (v.flip) hintTileFlip else null)
                ctx.drawText(pg, v.life.value, hintTextLife)
                ctx.drawText(pg, v.energy, hintTextEnergy)
                if (v.isHided) ctx.drawTile(pg, tileHide)
            }
        }
    }

    override val tilesEditAdd = onHerds.map { it.tlsVoin.neut }

    override fun editAdd(pg: Pg, side: Side, num: Int) {
        onHerds[num].grid()[pg] = VoinSimple(Life(5), side, pg.x > pg.pgser.xr / 2)
    }

    override fun editRemove(pg: Pg): Boolean {
        onHerds.forEach { if (it.grid().remove(pg)) return true }
        return false
    }

    override fun editChange(pg: Pg, side: Side) {
        onHerds.forEach { it.grid()[pg]?.let { v -> v.side = sideAfterChange(v.side, side) } }
    }

    override fun editDestroy(pg: Pg) {

    }
}



interface OnHerd:Ext {
    val tlsVoin: TlsVoin
    val grid: () -> Grid<VoinSimple>
}

class AimerVoin(r: Resource) : OnStopAim {
    val tlsMove = r.tlsAktMove
//    override fun stopMove(pgFrom: Pg, pgTo: Pg): Boolean {
//        return pgTo in grid()
//    }
}

class Life(valueInit: Int) {
    var value: Int = valueInit
        private set

    fun alter(d: Int) {
        value += d
    }

    override fun toString() = "Life($value)"
}