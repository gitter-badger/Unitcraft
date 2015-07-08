package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.server.Err
import unitcraft.server.Side

class Sider(val objs:()->Objs){

    fun get(obj: Obj, prop: PropertyMetadata): Side {
        return obj[prop.name] as Side
    }

    fun set(obj: Obj, prop: PropertyMetadata, v: Side) {
        obj[prop.name] = v
    }

    fun editChange(pg: Pg,sideVid:Side) {
        objs().filterIsInstance<ObjOwn>().byPg(pg).firstOrNull()?.let {
            it.side = when (it.side) {
                Side.n -> sideVid
                sideVid -> sideVid.vs
                sideVid.vs -> Side.n
                else -> throw Err("assertion")
            }
        }
    }
}