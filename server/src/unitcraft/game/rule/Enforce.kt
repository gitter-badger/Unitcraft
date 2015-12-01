package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Side
import java.util.ArrayList

class Enforce(r: Resource) {
    val tls = r.tlsBool("enforced", "enforcedAlready")
    val stager: Stager by inject()
    val drawer: Drawer by inject()
    val spoter: Spoter by inject()
    val objs: () -> Objs  by injectObjs()

    init {
        injectValue<Solider>().drawObjs.add { obj,side,ctx  -> if(obj.has<DataEnforce>()) ctx.drawTile(obj.pg,tls(obj<DataEnforce>().isOn)) }
        stager.slotEndTurn.add(1,"Enforce: удаляет статус Enforce у всех юнитов")  { objs().forEach { it.remove<DataEnforce>() } }
        spoter.listCanAkt.add { side, obj -> obj.has<DataEnforce>() && obj<DataEnforce>().isOn }
    }

    fun canEnforce(pg: Pg,sideVid: Side) = objs()[pg]?.let{ it.side!=sideVid && it.isVid(sideVid) && !it.has<DataEnforce>() }?:false

    fun enforce(pg: Pg) {
        objs()[pg]?.data(DataEnforce(true))
    }

    private class DataEnforce(var isOn:Boolean):Data
}



