package unitcraft.game

import java.util.*

class Slot<T>(val title: String) {
    private val list = ArrayList<EntrySlot<(T) -> Unit>>()

    fun add(prior: Int, desc: String, fn: (T) -> Unit) {
        list.add(EntrySlot(prior, desc, fn))
        Collections.sort(list)
    }

    fun each(fn: (Int,String)->Unit) {
        list.forEach { fn(it.prior,it.desc) }
    }

    fun exe(prm: T) {
        list.forEach { it.fn(prm) }
    }

    private data class EntrySlot<F>(val prior: Int, val desc: String, val fn: F) : Comparable<EntrySlot<F>> {
        override fun compareTo(other: EntrySlot<F>) = compareValues(prior, other.prior)
    }
}

