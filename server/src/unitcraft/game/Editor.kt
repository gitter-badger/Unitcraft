package unitcraft.game

import unitcraft.game.rule.ObjOwn
import unitcraft.game.rule.Objs
import unitcraft.game.rule.byPg
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import java.util.ArrayList
import kotlin.properties.Delegates


class Editor(val objs: () -> Objs) {
    private val groupTiles = ArrayList<List<Int>>()
    private val editAdds = ArrayList<(Pg, Side, Int) -> Unit>()
    private val editRemoves = ArrayList<(Pg) -> Boolean>()
    private val ranges = ArrayList<Range<Int>>()

    val opterTest by Delegates.lazy { Opter(groupTiles.flatten().map { Opt(DabTile(it)) }) }

    fun editAdd(pg: Pg, side: Side, num: Int) {
        editAdds[select(num)](pg, side, idxRel(num))
    }

    fun editRemove(pg: Pg) {
        editRemoves.forEach { if (it(pg)) return }
    }

    fun editDestroy(pg: Pg) {

    }

    fun editChange(pg: Pg, sideVid: Side) {
        objs().filterIsInstance<ObjOwn>().byPg(pg).firstOrNull()?.let {
            it.side = when (it.side) {
                Side.n -> sideVid
                sideVid -> sideVid.vs
                sideVid.vs -> Side.n
                else -> throw Err("unknown side=${it.side}")
            }
        }
    }

    fun onEdit(tiles: List<Int>, editAdd: (Pg, Side, Int) -> Unit, editRemove: (Pg) -> Boolean) {
        groupTiles.add(tiles)
        editAdds.add(editAdd)
        editRemoves.add(0, editRemove)
        val s = if(ranges.isNotEmpty()) ranges.last().end+1 else 0
        ranges.add(s..s + tiles.lastIndex)
    }

    private fun select(num: Int) = ranges.idxOfFirst { num in it }!!
    private fun idxRel(num: Int) = num - ranges[select(num)].start

}