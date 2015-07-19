package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Spoter
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Sider(spoter: Spoter,val objs: () -> Objs){
    private val side = "side"

    init{
        spoter.listCanAkt.add { side, obj -> obj.side == side }
    }

    fun change(obj:Obj,side:Side){
        obj.side=side
    }

    fun isEnemy(obj:Obj,side:Side) = obj.side == side.vs
    fun isAlly(obj:Obj,side:Side) = obj.side == side




}