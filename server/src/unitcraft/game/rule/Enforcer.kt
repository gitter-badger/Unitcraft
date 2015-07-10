package unitcraft.game.rule

import unitcraft.game.Shaper
import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Stager
import java.util.*

class Enforcer(r: Resource, val stager: Stager, val drawerVoin: DrawerVoin, val objs: () -> Objs) {
    private val enforced = "enforced"
    val tls = r.tlsBool("enforced", "enforcedAlready")
    val kinds = ArrayList<Kind>()
    
    init {
        drawerVoin.tileStts.add { voin,side -> voin[enforced]?.let { tls(it as Boolean) } }
        stager.onEndTurn {
            objs().forEach { it.remove(enforced) }
        }
    }

    fun canEnforce(pg: Pg) = objs().byPg(pg).byKind(kinds).filter{it[enforced]!=null}.firstOrNull() != null


    fun enforce(pg: Pg) {
        objs().byPg(pg).byKind(kinds).sortBy{it.shape.zetOrder}.firstOrNull()?.let {
            it[enforced] = true
        }
    }
}









