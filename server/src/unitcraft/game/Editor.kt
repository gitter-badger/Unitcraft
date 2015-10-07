package unitcraft.game

import unitcraft.game.rule.Objs
import unitcraft.game.rule.Singl
import unitcraft.game.rule.byPg
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import java.util.*
import kotlin.properties.Delegates


class Editor {
    private val edits = ArrayList<Edit>()

    private val groupTiles by lazy(LazyThreadSafetyMode.NONE) {
        sort()
        edits.map{it.tiles}
    }

    private val editAdds by lazy(LazyThreadSafetyMode.NONE) {
        sort()
        edits.map{it.editAdd}
    }

    private val editRemoves by lazy(LazyThreadSafetyMode.NONE) {
        sort()
        edits.reversed().map{it.editRemove}
    }

    val opterTest by lazy(LazyThreadSafetyMode.NONE) {
        sort()
        Opter(groupTiles.flatten().map { Opt(DabTile(it)) })
    }

    private val ranges by lazy(LazyThreadSafetyMode.NONE) {
        sort()
        ArrayList<Range<Int>>().apply{
            for(tiles in edits.map{it.tiles}) {
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

    //editor.onEdit(PriorDraw.place,TpPlace.values().map{tiles[it]!!.first()},{pg,side,num -> flats()[pg].tpPlace = unitcraft.game.TpPlace.values()[num]},{false})
    fun onEdit(priorDraw:PriorDraw,tiles: List<Tile>, editAdd: (Pg, Side, Int) -> Unit, editRemove: (Pg) -> Boolean) {
        edits.add(Edit(priorDraw,tiles,editAdd,editRemove))
    }

    private var ready = false
    private fun sort(){
        if(ready) return
        Collections.sort(edits,compareBy{it.priorDraw})
        ready = true
    }

    private fun select(num: Int) = ranges.idxOfFirst { num in it }!!
    private fun idxRel(num: Int) = num - ranges[select(num)].start

    class Edit(val priorDraw:PriorDraw,val tiles: List<Tile>, val editAdd: (Pg, Side, Int) -> Unit, val editRemove: (Pg) -> Boolean)
}