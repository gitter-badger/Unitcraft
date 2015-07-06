package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class Staziser(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd,OnRaise{
    override val tlsVoin = r.tlsVoin("staziser")
    val tlsMove = r.tlsAktMove

    override fun focus() = grid().map{it.key to it.value.side}.toList()

    override fun raise(aim: Aim, pgSpot: Pg, pgSrc: Pg, side: Side,r:Raise) {
        if(pgSrc in grid()) for (pgNear in pgSpot.near) {
            if (aim.canMove(pgSpot, pgNear)) r.add(pgNear, tlsMove){ make ->
                make.move(pgSpot,pgNear)
                make.minusEnergy(pgSpot)
            }
        }
    }
}