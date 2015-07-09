package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Stager
import unitcraft.game.rule.DrawerVoin
import unitcraft.game.rule.Objs
import unitcraft.game.rule.Voin
import unitcraft.game.rule.byPg

class Enforcer(r: Resource, val stager: Stager, val drawerVoin: DrawerVoin, val objs: () -> Objs) {

    val tls = r.tlsBool("enforced", "enforcedAlready")

    init {
        drawerVoin.addTileStt { voin -> voin.enforced?.let { tls(it) } }
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