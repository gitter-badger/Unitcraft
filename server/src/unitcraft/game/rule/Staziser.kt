package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side

class Staziser(r: Resource,val stazis:Stazis, override val grid: () -> Grid<VoinSimple>) : OnHerd, OnRaise {
    override val tlsVoin = r.tlsVoin("staziser")
    val tlsAkt = r.tlsAkt("staziser")
    val tlsMove = r.tlsAktMove

    override fun sideSpot(pg: Pg) = grid()[pg]?.side

    override fun spot(arm: Arm, pgSpot: Pg,pgSrc: Pg, sideVid: Side, s: Spot) {
        grid()[pgSrc]?.let { voin ->
            for (pgNear in pgSpot.near) {
                if(arm.canSkil(pgSpot, pgNear, sideVid)){
                    s.add(pgNear, tlsAkt) {
                        stazis.plant(pgNear)
                        voin.energy = 0
                    }
                }
            }
            for (pgNear in pgSpot.near) {
                val reveal = arm.canMove(Move(pgSpot, pgNear, TpMove.unit, false, sideVid))
                if (reveal != null) {
                    s.add(pgNear, tlsMove) {
                        if (reveal()) {
                            move(voin,pgSpot,pgNear)
                            voin.energy -= 1
                        }
                    }
                }
            }
        }
    }

    private fun move(voin:VoinSimple,pgFrom:Pg,pgTo:Pg){
        grid().move(pgFrom, pgTo)
        val xd = pgFrom.x - pgTo.x
        if (xd != 0) voin.flip = xd > 0
    }
}

class Imitator(r:Resource,override val grid:()->Grid<VoinSimple>) : OnHerd, OnRaise {
    override val tlsVoin = r.tlsVoin("imitator")

    override fun sideSpot(pg: Pg) = grid()[pg]?.side

    override fun spotByCopy(pgSpot:Pg): List<Pg> {
        return pgSpot.near
    }
}