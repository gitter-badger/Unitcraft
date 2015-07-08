package unitcraft.game

import unitcraft.game.rule.ObjOwn
import unitcraft.game.rule.Objs
import unitcraft.game.rule.byPg
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.*
import kotlin.properties.Delegates


class Editor(val objs:()-> Objs){
    val editAdds = ArrayList<Pair<Int, (Pg, Side) -> Unit>>()
    val editRemoves = ArrayList<(Pg) ->Boolean>()

    val opterTest by Delegates.lazy{ Opter(editAdds.map { Opt(DabTile(it.first))})}

    fun editAdd(pg: Pg, side: Side, num: Int) {
        editAdds[num].second(pg,side)
    }

    fun editRemove(pg: Pg) {
        editRemoves.forEach { if(it(pg)) return }
    }

    fun editDestroy(pg: Pg) {

    }

    fun editChange(pg: Pg,sideVid:Side) {
        objs().filterIsInstance<ObjOwn>().byPg(pg).firstOrNull()?.let {
            it.side = when (it.side) {
                Side.n -> sideVid
                sideVid -> sideVid.vs
                sideVid.vs -> Side.n
                else -> throw Err("unknown side=${it.side}")
            }
        }
    }

    fun onEdit(tile: Int, editAdd: (Pg, Side) ->Unit, editRemove: (Pg) ->Boolean) {
        editAdds.add(tile to editAdd)
        editRemoves.add(0,editRemove)
    }
}