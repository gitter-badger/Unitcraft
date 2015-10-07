package unitcraft.server

inline fun <T> idxsMap(qnt:Int,f: (Int) -> T) = (0..qnt-1).map{f(it)}

inline fun <T> List<T>.idxOfFirst(predicate: (T) -> Boolean): Int? {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return null
}

inline fun <T> MutableIterable<T>.exclude(predicate: (T) -> Boolean) {
    val iter = iterator()
    for (elem in iter) if(predicate(elem)) iter.remove()
}
