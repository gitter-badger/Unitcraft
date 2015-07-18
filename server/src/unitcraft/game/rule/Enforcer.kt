package unitcraft.game.rule

import unitcraft.game.*
import java.util.ArrayList

class Enforcer(r: Resource, val stager: Stager, val drawer: Drawer, val spoter: Spoter, val objs: () -> Objs) {
    val tls = r.tlsBool("enforced", "enforcedAlready")

    init {
        drawer.drawObjs.add { obj,side,ctx  -> if(obj.has<Enforce>()) ctx.drawTile(obj.head(),tls(obj<Enforce>().isOn)) }
        stager.onEndTurn { objs().forEach { it.remove<Enforce>() } }
        spoter.listCanAkt.add { side, obj -> obj.has<Enforce>() && obj<Enforce>().isOn }
    }

    fun canEnforce(pg: Pg) = objs()[pg]?.let{ !it.has<Enforce>() }?:false

    fun enforce(pg: Pg) {
        objs()[pg]?.data(Enforce(true))
    }

    class Enforce(var isOn:Boolean):Data
}



