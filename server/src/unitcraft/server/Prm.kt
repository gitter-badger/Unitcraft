package unitcraft.server

import java.util.regex.Pattern


class Prm(private val s: String) {
    private val p = s.split(' ')

    fun ensureEmpty() {
        if (s.isNotEmpty()) throw Violation("prm must be empty: $s")
    }

    fun ensureSize(size: Int) {
        if (size != p.size) throw Violation("prm size !=$size: $s")
    }

    fun int(pos: Int, sizeMax: Int): Int {
        if (pos >= p.size) throw Violation("pos($pos) is out: $s")
        if (p[pos].length > sizeMax) throw Violation("size of int at ($pos) > $sizeMax: $s")
        if (!p[pos].allDigits()) throw Violation("invalid int at ($pos): $s")
        return p[pos].toInt()
    }

    fun bet(pos: Int): Int {
        if (pos >= p.size) throw Violation("pos($pos) is out: $s")
        if (p[pos].length > 6) throw Violation("size of bet at ($pos) > 6: $s")
        if (!p[pos].allDigits()) throw Violation("invalid bet int at ($pos): $s")
        return p[pos].toInt()
    }
    fun mission(pos: Int): Int {
        if (pos >= p.size) throw Violation("pos($pos) is out: $s")
        if (p[pos].length > 2) throw Violation("size of mission at ($pos) > 2: $s")
        if (!p[pos].allDigits()) throw Violation("invalid mission int at ($pos): $s")
        return p[pos].toInt()
    }
    fun str(pos: Int, sizeMin: Int, sizeMax: Int): String {
        if (pos >= p.size) throw Violation("pos($pos) is out: $s")
        if (p[pos].length < sizeMin) throw Violation("size of string at ($pos) < $sizeMin: $s")
        if (p[pos].length > sizeMax) throw Violation("size of string at ($pos) > $sizeMax: $s")
        return p[pos]
    }

    fun akt(): Pair<Int, String> {
        val p = s.split('#')
        if (p.size != 2) throw Violation("invalid akt prm: $s")
        if (p[0].length > 6) throw Violation("size of version > 6: $s")
        if (!p[0].allDigits()) throw Violation("invalid version prm: $s")
        return Pair(p[0].toInt(), p[1])
    }


    companion object {
        private val onlyDigits = Pattern.compile("""\d+""")
        fun String.allDigits(): Boolean {
            return onlyDigits.matcher(this).matches()
        }
    }
}