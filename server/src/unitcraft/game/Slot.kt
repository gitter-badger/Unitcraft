package unitcraft.game

import java.util.*

class Slot<T:Aide>(override val title: String):SectionRule{
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

    override fun each(fn: (Int?,String)->Unit) {
        list.forEach { fn(it.prior,it.desc) }
    }
}

class Slop<T:Aide>(override val title: String):SectionRule{
    private val list = ArrayList<EntrySlot<T>>()

    fun add(module:Any, desc: String, fn: T.() -> Boolean) {
        list.add(EntrySlot(module.javaClass.simpleName+": "+desc, fn))
    }

    private data class EntrySlot<T:Aide>(val desc: String, val fn: T.() -> Boolean)

    fun pass(prm: T) = !list.any { prm.(it.fn)() }

    override fun each(fn: (Int?,String)->Unit) {
        list.forEach { fn(null,it.desc) }
    }
}

interface Aide

interface SectionRule{
    val title:String
    fun each(fn: (Int?,String)->Unit)
}