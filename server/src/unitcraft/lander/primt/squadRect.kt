package unitcraft.lander

import unitcraft.game.Pg

// юниты располагаются в двух прямоугольниках, которые удалены друг от друга на distMin
fun squadRect(qnt: Int, distMin: Int) = primt {

//    fun inRect(pg: Pg, list: List<Pg>) = list.all { it -> pg.distance(it) >= distMin }
//    var i = 0
//    while (i < qnt) {
//        val pg = rnd(ppp { isFree(it) && isFar(it, ppp { isAux(it) }) }) ?: break
//        lay(pg)
//        val pg1 = rnd(ppp { isFree(it) && isFar(it, ppp { isLay(it) }) }) ?: break
//        aux(pg1)
//        i += 1
//    }
}

fun main(args: Array<String>) {
    play(squadRect(10, 5))
}