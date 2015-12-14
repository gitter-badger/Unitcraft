package unitcraft.lander

import unitcraft.game.Pg
import unitcraft.lander.Player

import java.util.*

val river = primt {
    fun isCnn(start: Pg, finish: Pg, where: (Pg) -> Boolean): Boolean {
        val wave = ArrayList<Pg>()
        val que = ArrayList<Pg>()
        que.add(start)
        wave.add(start)
        while (true) {
            if (que.isEmpty()) break
            val next = que.removeAt(0).near.filter { where(it) && it !in wave }
            que.addAll(next)
            wave.addAll(next)
        }
        return finish in wave
    }

    val start = pgRnd { it.isEdge() } ?: return@primt
    val finish = pgRnd { it.isEdge() && it != start && it.distance(start) > 5 } ?: return@primt
    if (!isCnn(start, finish) { !isExc(it) }) return@primt
    lay(ppp())
    val pgsMust = ArrayList<Pg>()
    while (true) {
        val next = pgRnd { it != start && it != finish && isLay(it) && it !in pgsMust } ?: break
        if (isCnn(start, finish) { isLay(it) && it != next }) unlay(next)
        else pgsMust.add(next)
    }
}

fun main(args: Array<String>) {
    play(river)
}
