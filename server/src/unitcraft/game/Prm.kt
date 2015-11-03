package unitcraft.game

import java.util.ArrayList
import unitcraft.server.Side
import unitcraft.land.Land
import unitcraft.server.Violation
import java.util.regex.Pattern

class Prm(private val pgser: Pgser, private val s: String) {
    private val strs = if(s.isEmpty()) emptyList<String>() else s.split(' ').toList()

    init {
        if (!strs.all { it.allDigits() }) throw Violation("prm have non-ints in cmd: $s")
        if (!strs.all { it.length <= 3 }) throw Violation("size of ints in cmd is too big : $s")
    }

    private val p = strs.map { it.toInt() }

    fun pg(i: Int): Pg {
        if (i + 1 >= p.size) throw Violation("not enough size for pg at pos($i) in cmd: $s")
        return pgser.pgOrNull(p[i], p[i + 1]) ?: throw Violation("prm pg at $i is out in cmd: $s")
    }

    fun int(pos: Int): Int {
        if (pos >= p.size) throw Violation("pos($pos) is out: $s")
        return p[pos]
    }

    fun ensureSize(size: Int) {
        if (size != p.size) throw Violation("prm size($s) != $size")
    }

    companion object {
        private val onlyDigits = Pattern.compile("""\d+""")
        private fun String.allDigits(): Boolean {
            return onlyDigits.matcher(this).matches()
        }
    }
}