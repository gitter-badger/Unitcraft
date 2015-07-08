package unitcraft.game

import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.*
import kotlin.properties.Delegates


class Editor{
    val edits = ArrayList<Triple<Int, (Pg, Side) -> Unit,(Pg) ->Boolean>>()
    val editsReverse by Delegates.lazy{ edits.map{it.third}.reverse()}

    val opterTest by Delegates.lazy{ Opter(edits.map { Opt(DabTile(it.first))})}

    fun editAdd(pg: Pg, side: Side, num: Int) {
        edits[num].second(pg,side)
    }

    fun editRemove(pg: Pg) {
        editsReverse.forEach { if(it(pg)) return }
    }

    fun editDestroy(pg: Pg) {

    }

    fun onEdit(tile: Int, editAdd: (Pg, Side) ->Unit, editRemove: (Pg) ->Boolean) {
        edits.add(Triple(tile, editAdd,editRemove))
    }
}