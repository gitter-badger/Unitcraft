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

class Enforcer(r: Resource, val stager: Stager, val drawerVoin: DrawerVoin, val objs: () -> Objs) {

    val tls = r.tlsBool("enforced", "enforcedAlready")

    init {
        drawerVoin.regTileStt { voin -> voin.enforced?.let { tls(it) } }
        stager.onEndTurn {
            objs().filterIsInstance<Voin>().forEach { it.enforced = null }
        }
    }

    fun get(obj: Voin, prop: PropertyMetadata): Boolean? {
        return obj[prop.name] as Boolean?
    }

    fun set(obj: Voin, prop: PropertyMetadata, v: Boolean?) {
        if (v == null) obj.remove(prop.name)
        else obj[prop.name] = v
    }

    fun canEnforce(pg: Pg) = objs().filterIsInstance<Voin>().byPg(pg).firstOrNull() != null


    fun enforce(pg: Pg) {
        objs().filterIsInstance<Voin>().byPg(pg).firstOrNull()?.let {
            it.enforced = true
        }
    }
}

enum class Kind {
    inviser
}



