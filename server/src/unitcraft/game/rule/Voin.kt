package unitcraft.game.rule

import sideAfterChange
import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class VoinSimple(val life:Life,var side:Side,var flip: Boolean){
    var isHided:Boolean = false
    var energy = 3
}

open class HelpVoin(rr:Resource,val r:ResDrawSimple,name:String,val grid:()->Grid<VoinSimple>):OnDraw,OnEdit,OnStopAim{

    val tlsVoin = rr.tlsVoin(name)

    override val prior = OnDraw.Prior.voin

    override fun draw(side: Side, ctx: CtxDraw) {
        for((pg,v) in grid()){
            ctx.drawTile(pg,tlsVoin(side,v.side))
            ctx.drawText(pg,v.life.value,r.hintTextLife)
            ctx.drawText(pg,v.energy,r.hintTextEnergy)
            if(v.isHided) ctx.drawTile(pg,r.tileHide)
        }
    }

    override val tileEditAdd = tlsVoin.neut

    override fun editAdd(pg: Pg, side: Side) {
        grid()[pg] = VoinSimple(Life(5),side,pg.x > pg.pgser.xr / 2)
    }

    override fun editRemove(pg: Pg) = grid().remove(pg)

    override fun stopMove(pgFrom: Pg, pgTo: Pg): Boolean {
        return pgTo in grid()
    }

    override fun editChange(pg: Pg, side: Side) {
        grid()[pg]?.let{ v -> v.side = sideAfterChange(v.side,side)}
    }

    override fun editDestroy(pg: Pg) {

    }
}

class ResDrawSimple(r: Resource) {
    val tlsMove = r.tlsAktMove
    val tileHide = r.tile("hide")
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
}

class Life(valueInit:Int){
    var value:Int = valueInit
        private set

    fun alter(d:Int){
        value += d
    }

    override fun toString() = "Life($value)"
}