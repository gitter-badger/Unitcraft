package unitcraft.lander

fun formationChess(qnt: Int, distEdge: Int) = prism {
    var i = 0
    while (i < qnt) {
        lay(rnd(ppp { isFree(it) && it.x<distEdge }) ?: break)
        aux(rnd(ppp { isFree(it) && xl-distEdge<it.x }) ?: break)
        i += 1
    }
}

fun main(args: Array<String>) {
    play(formationChess(10, 3))
}