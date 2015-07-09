package unitcraft.game.rule

import unitcraft.game.Grid
import unitcraft.game.Pg
import unitcraft.game.Raiser
import unitcraft.game.Resource

class Imitator(r: Resource,val raiser: Raiser,val drawerVoin:DrawerVoin,val editorVoin:EditorVoin) {
    val tls = r.tlsVoin("imitator")
    init{
        drawerVoin.addKind(KindImitator,tls)
        editorVoin.addKind(KindImitator,tls.neut)
    }
    //fun sideSpot(pg: Pg) = grid()[pg]?.side

//    fun spotByCopy(pgSpot: Pg): List<Pg> {
//        return pgSpot.near
//    }
}

object KindImitator:Kind()