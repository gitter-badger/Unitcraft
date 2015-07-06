package unitcraft.game

import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.*
import kotlin.reflect.jvm.kotlin

class Editor(exts:List<Ext>){
    val edits = exts.filterIsInstance<OnEdit>().sortBy{ it.prior }
    val editsReverse = exts.filterIsInstance<OnEdit>().sortDescendingBy{ it.prior }

    val opterTest = Opter(edits.flatMap { it.tilesEditAdd }.map { Opt(DabTile(it))})

    private val ranges = ArrayList<Range<Int>>().init {
        var sum = 0
        for (chance in edits.map { it.tilesEditAdd.size() }) {
            add((sum..sum + chance - 1))
            sum += chance
        }
    }

    private fun select(num: Int) = ranges.idxOfFirst { num in it }!!
    private fun idxRel(num: Int) = num - ranges[select(num)].start

    fun editAdd(pg: Pg, side: Side, num: Int) {
        edits[select(num)].editAdd(pg,side,idxRel(num))
    }

    fun editRemove(pg: Pg) {
        editsReverse.forEach { if(it.editRemove(pg)) return }
    }

    fun editChange(pg: Pg, side: Side) {
        edits.forEach { it.editChange(pg,side) }
    }

    fun editDestroy(pg: Pg) {

    }
}

interface OnEdit:Ext{
    val prior: OnDraw.Prior
    val tilesEditAdd:List<Int>
    fun editAdd(pg:Pg, side:Side, num: Int)
    fun editRemove(pg: Pg):Boolean
    fun editChange(pg:Pg, side:Side){}
    fun editDestroy(pg:Pg){}
}