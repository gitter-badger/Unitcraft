package unitcraft.lander

fun spot(qnt:Int) = prism {
    pgRnd { true }?.apply{lay(this)}?: return@prism
    var i = 1
    while (i < qnt) {
        rnd(ppp {isLay(it)}.flatMap { it.near }.filterNot { isExc(it) || isLay(it) })?.let { lay(it) } ?: break
        i += 1
    }
}

fun main(args: Array<String>) {
    play(spot(10))
}


