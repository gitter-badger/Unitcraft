package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class Staziser(r:Resource,resDrawSimple:ResDrawSimple,grid:()->Grid<VoinSimple>):
        HelpVoin(r,resDrawSimple,"staziser",grid),OnRaise{

    val tlsMove = r.tlsAktMove

    override fun focus() = grid().map{it.key to it.value.side}.toList()

    override fun raise(aim: Aim, pgSpot: Pg, pgSrc: Pg, side: Side): List<PreAkt> {
        val list = ArrayList<PreAkt>()
        if(pgSrc in grid()) for (pgNear in pgSpot.near) {
            if (aim.canMove(pgSpot, pgNear)) list.add(PreAkt(pgNear, tlsMove){ make ->
                make.move(pgSpot,pgNear)
                make.minusEnergy(pgSpot)
            })
        }
        return list
    }
}