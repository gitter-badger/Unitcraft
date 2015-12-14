package unitcraft.lander

fun spray(qnt:Int) = prism {
    var i = 0
    while (i < qnt) {
        val pg = rnd(ppp().filter { isFree(it) }) ?: break
        lay(pg)
        i += 1
    }
}

fun main(args: Array<String>) {
    play(spray(10))
}