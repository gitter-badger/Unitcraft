package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Spoter
import unitcraft.game.Stager
import java.util.ArrayList

class Enforcer(r: Resource, val stager: Stager, val drawerObj: DrawerObj, val spoter: Spoter, val objs: () -> Objs) {
    val tls = r.tlsBool("enforced", "enforcedAlready")

    init {
        drawerObj.tileStts.add { obj, side -> if(obj.has<Enforce>()) tls(obj<Enforce>().isOn) else null }
        stager.onEndTurn { objs().forEach { it.remove<Enforce>() } }
        spoter.listCanAkt.add { side, obj -> obj.has<Enforce>() && obj<Enforce>().isOn }
    }

    fun canEnforce(pg: Pg) = objs()[pg]?.let{ it.has<Enforce>() }?:false

    fun enforce(pg: Pg) {
        objs()[pg]?.data(Enforce(true))
    }

    class Enforce(var isOn:Boolean):Data()
}



