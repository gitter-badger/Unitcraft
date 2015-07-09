package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Stager

class Telepath(r: Resource, val enforcer: Enforcer,val drawerVoin:DrawerVoin,val editorVoin:EditorVoin) {
    val tlsVoin = r.tlsVoin("enforcer")
    val tlsAkt = r.tlsAkt("enforcer")
    val tlsMove = r.tlsAktMove

    init {
        drawerVoin.addKind(KindTelepath,tlsVoin)
        editorVoin.addKind(KindTelepath,tlsVoin.neut)
    }
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







