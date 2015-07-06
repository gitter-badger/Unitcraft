package unitcraft.game

import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.*
import kotlin.reflect.jvm.kotlin

class Editor(exts:List<Ext>){
    val edits = exts.filterIsInstance<OnEdit>().sortBy{ it.prior }
    val editsReverse = exts.filterIsInstance<OnEdit>().sortDescendingBy{ it.prior }

    val opterTest = Opter(edits.map { it.tileEditAdd }.map { Opt(DabTile(it))})

    fun editAdd(pg: Pg, side: Side, num: Int) {
        edits[num].editAdd(pg,side)
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
    val tileEditAdd:Int
    fun editAdd(pg:Pg, side:Side)
    fun editRemove(pg: Pg):Boolean
    fun editChange(pg:Pg, side:Side){}
    fun editDestroy(pg:Pg){}
}