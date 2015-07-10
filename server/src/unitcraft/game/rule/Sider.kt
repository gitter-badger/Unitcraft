package unitcraft.game.rule

import unitcraft.server.Side

class Sider{
    private val side = "side"
    fun side(obj:Obj) = obj[side] as Side
    fun isEnemy(obj:Obj,side:Side) = side(obj:Obj) == side.vs
}