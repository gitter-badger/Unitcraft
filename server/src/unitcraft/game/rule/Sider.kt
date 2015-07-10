package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Sider(val objs: () -> Objs){
    private val side = "side"

    val kinds = ArrayList<Kind>()



    fun side(obj:Obj) = obj[side] as Side

    fun change(obj:Obj,side:Side){
        obj[this.side]=side
    }

    fun capture(obj:Obj,target:Obj){
        target[side] = side(obj)
    }


    fun isEnemy(obj:Obj,side:Side) = side(obj:Obj) == side.vs
    fun isAlly(obj:Obj,side:Side) = side(obj:Obj) == side

    fun editChange(pg: Pg, sideVid: Side) {
        objs().byPg(pg).byKind(kinds).firstOrNull()?.let {
            change(it,when (side(it)) {
                Side.n -> sideVid
                sideVid -> sideVid.vs
                sideVid.vs -> Side.n
                else -> throw Err("unknown side=${side(it)}")
            })
        }
    }
}