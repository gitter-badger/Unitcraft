package unitcraft.lander

import unitcraft.game.Pg

fun squadSpray(distMin: Int) = primt {
    fun isFar(pg: Pg, list: List<Pg>) = list.all { it -> pg.distance(it) >= distMin }

    while (true) {
        lay(rnd(ppp { isFree(it) && isFar(it, ppp { isAux(it) }) }) ?: break)
        aux(rnd(ppp { isFree(it) && isFar(it, ppp { isLay(it) }) }) ?: break)
    }
}

fun main(args: Array<String>) {
    play(squadSpray(5))
}