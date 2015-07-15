package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import unitcraft.game.Spoter
import unitcraft.game.Stager
import java.util.ArrayList

class Enforcer(r: Resource, val stager: Stager, val drawerVoin: DrawerVoin, spoter: Spoter, val objs: () -> Objs) {
    private val enforced = "enforced"
    val tls = r.tlsBool("enforced", "enforcedAlready")
    val kinds = ArrayList<Kind>()

    init {
        drawerVoin.tileStts.add { voin, side -> voin[enforced]?.let { tls(it as Boolean) } }
        stager.onEndTurn { objs().forEach { it.remove(enforced) } }
        spoter.listCanAkt.add { side, obj -> enforced(obj)==true }
    }

    private fun enforced(obj: Obj) = obj[enforced] as Boolean?

    fun canEnforce(pg: Pg) = objs().byPg(pg).byKind(kinds).filter { it[enforced] == null }.firstOrNull() != null

    fun enforce(pg: Pg) {
        val aim = objs().byPg(pg).byKind(kinds).sortDescendingBy { it.shape.zetOrder }.firstOrNull()
        if(spoter.hasSkil(obj)) {
            val tag = TagEnforce(true)
            drawer.onDraw(tag){ tls(tag.enforced) }
            aim.tag(tag)
        }

        objs().byPg(pg).byKind(kinds).sortDescendingBy { it.shape.zetOrder }.firstOrNull()?.let {
            it[enforced] = true
        }
    }

}

class TagEnforce(var enforced:Boolean? = null ):Tag()

open class Tag




