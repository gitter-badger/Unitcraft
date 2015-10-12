package unitcraft.game.rule

import unitcraft.game.Spoter
import unitcraft.inject.inject
import unitcraft.server.Side

class Sider {
    val objs: () -> Objs by inject()
    val spoter: Spoter by inject()

    private val side = "side"

    init {
        spoter.listCanAkt.add { side, obj -> obj.side == side }
    }

    fun change(obj: Obj, side: Side) {
        obj.side = side
    }

    fun isEnemy(obj: Obj, side: Side) = obj.side == side.vs
    fun isAlly(obj: Obj, side: Side) = obj.side == side


}