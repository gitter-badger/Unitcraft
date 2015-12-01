package unitcraft.game

import java.util.*

class Slot<T:Aide>(val title: String){
    private val list = ArrayList<EntrySlot<T>>()

    fun add(prior: Int, module:Any, desc: String, fn: T.() -> Unit) {
        list.add(EntrySlot(prior, module.javaClass.simpleName+": "+desc, fn))
        Collections.sort(list)
    }

    private data class EntrySlot<T:Aide>(val prior: Int, val desc: String, val fn: T.() -> Unit) : Comparable<EntrySlot<*>> {
        override fun compareTo(other: EntrySlot<*>) = compareValues(prior, other.prior)
    }

    fun exe(prm: T) {
        list.forEach { prm.(it.fn)() }
    }

    fun each(fn: (Int,String)->Unit) {
        list.forEach { fn(it.prior,it.desc) }
    }
}

interface Aide
