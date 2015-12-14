package unitcraft.lander

import unitcraft.game.Pg

fun sprayFormation(qnt: Int, distMin: Int) = prism {
    fun isFar(pg: Pg, list: List<Pg>) = list.all { it -> pg.distance(it) >= distMin }
    var i = 0
    while (i < qnt) {
        val pg = rnd(ppp { isFree(it) && isFar(it, ppp { isAux(it) }) }) ?: break
        lay(pg)
        val pg1 = rnd(ppp { isFree(it) && isFar(it, ppp { isLay(it) }) }) ?: break
        aux(pg1)
        i += 1
    }
}

fun main(args: Array<String>) {
    play(sprayFormation(10, 5))
}