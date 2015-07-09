package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Stager

class Telepath(r: Resource, val enforcer: Enforcer) {
    val tlsVoin = r.tlsVoin("enforcer")
    val tlsAkt = r.tlsAkt("enforcer")
    val tlsMove = r.tlsAktMove

//    fun spot() {
//        for (pgNear in pgSpot.near)
//            if (enforcer.canEnforce(pgNear)) {
//                s.add(pgNear, tlsAkt) {
//                    enforcer.enforce(pgNear)
//                    voin.energy = 0
//                }
//            }
//
//    }
}

object KindTelepath:Kind()







