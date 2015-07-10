package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Stager
import unitcraft.game.rule.DrawerVoin
import unitcraft.game.rule.Objs
import unitcraft.game.rule.Voin
import unitcraft.game.rule.byPg

class Enforcer(r: Resource, val stager: Stager, val drawerVoin: DrawerVoin, val objs: () -> Objs) {
    private val enforced = "enforced"
    val tls = r.tlsBool("enforced", "enforcedAlready")

    init {
        drawerVoin.addTileStt { voin -> voin[enforced]?.let { tls(it as Boolean) } }
        stager.onEndTurn {
            objs().filterIsInstance<Voin>().forEach { it.remove(enforced) }
        }
    }

    fun canEnforce(pg: Pg) = objs().filterIsInstance<Voin>().byPg(pg).filter{it[enforced]!=null}.firstOrNull() != null


    fun enforce(pg: Pg) {
        objs().filterIsInstance<Voin>().byPg(pg).sortBy{it.shape.zetOrder}.firstOrNull()?.let {
            it[enforced] = true
        }
    }
}