package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.server.Side
import java.util.ArrayList

class Enforcer(r: Resource) {
    val tls = r.tlsBool("enforced", "enforcedAlready")
    val stager: Stager by inject()
    val drawer: Drawer by inject()
    val spoter: Spoter by inject()
    val objs: () -> Objs  by injectObjs()

    init {
        drawer.drawObjs.add { obj,side,ctx  -> if(obj.has<Enforce>()) ctx.drawTile(obj.pg,tls(obj<Enforce>().isOn)) }
        stager.onEndTurn { objs().forEach { it.remove<Enforce>() } }
        spoter.listCanAkt.add { side, obj -> obj.has<Enforce>() && obj<Enforce>().isOn }
    }

    fun canEnforce(pg: Pg,sideVid: Side) = objs()[pg]?.let{ it.side!=sideVid && it.isVid(sideVid) && !it.has<Enforce>() }?:false

    fun enforce(pg: Pg) {
        objs()[pg]?.data(Enforce(true))
    }

    class Enforce(var isOn:Boolean):Data
}



