package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.WeakHashMap
import kotlin.reflect.jvm.kotlin

class Enforcer(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd,OnRaise{
    override val tlsVoin = r.tlsVoin("enforcer")
    val tlsAkt = r.tlsAkt("enforcer")
    val tlsMove = r.tlsAktMove

    override fun sideSpot(pg: Pg) = grid()[pg]?.side
    override fun spot(arm: Arm, pgSpot: Pg, pgSrc: Pg, sideVid: Side, s: Spot) {
        grid()[pgSrc]?.let { voin ->
            for (pgNear in pgSpot.near) {
                if(arm.canSkil(pgSpot, pgNear, sideVid)){
                    s.add(pgNear, tlsAkt) {
                        arm.addStt(pg,SttEnforcer())
                        voin.energy = 0
                    }
                }
            }
            for (pgNear in pgSpot.near) {
                val aim = arm.canMove(Move(pgSpot, pgNear, TpMove.unit, false, sideVid))
                if (aim != null) {
                    s.add(pgNear, tlsMove) {
                        aim.fire()
                        if (!aim.isBusy) {
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

class SttEnforcer{
    var isActive = true
}