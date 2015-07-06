package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class Staziser(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd,OnRaise{
    override val tlsVoin = r.tlsVoin("staziser")
    val tlsMove = r.tlsAktMove

    override fun sideSpot(pg:Pg) = grid()[pg]?.side

    override fun spot(aim: Aim, pgSpot: Pg, pgSrc: Pg, side: Side,s:Spot) {
        grid()[pgSrc]?.let { voin ->
            for (pgNear in pgSpot.near) {
//            if (aim.canMove(pgSpot, pgNear))

                s.add(pgNear, tlsMove){ make ->
                    //make.move(pgSpot,pgNear)
                    voin.energy -= 1
                }
            }
        }
    }
}