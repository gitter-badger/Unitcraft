package unitcraft.game

import unitcraft.game.rule.Objs
import unitcraft.game.rule.byPg
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.ArrayList
import kotlin.properties.Delegates


class Editor {
    private val groupTiles = ArrayList<List<Int>>()
    private val editAdds = ArrayList<(Pg, Side, Int) -> Unit>()
    private val editRemoves = ArrayList<(Pg) -> Boolean>()

    val opterTest by Delegates.lazy { Opter(groupTiles.flatten().map { Opt(DabTile(it)) }) }

    private val ranges by Delegates.lazy {ArrayList<Range<Int>>().init{
        for(tiles in groupTiles) {
            val s = if (isNotEmpty()) last().end + 1 else 0
            add(s..s + tiles.lastIndex)
        }
    }}

    fun editAdd(pg: Pg, side: Side, num: Int) {
        editAdds[select(num)](pg, side, idxRel(num))
    }

    fun editRemove(pg: Pg) {
        editRemoves.forEach { if (it(pg)) return }
    }

    fun editDestroy(pg: Pg) {

    }

    fun onEdit(tiles: List<Int>, editAdd: (Pg, Side, Int) -> Unit, editRemove: (Pg) -> Boolean) {
        groupTiles.add(tiles)
        editAdds.add(editAdd)
        editRemoves.add(0, editRemove)

    }

    private fun select(num: Int) = ranges.idxOfFirst { num in it }!!
    private fun idxRel(num: Int) = num - ranges[select(num)].start

}