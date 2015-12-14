package unitcraft.lander

val squadVersusCorner = primt {
    val xe = xr / 2 - 1
    val ye = yr / 2 - 1

    val xs = xe + 1 + xr % 2
    val ys = ye + 1 + yr % 2

    if(rndBln()) {
        lay(Rect(pgser.pg(0, 0), pgser.pg(xe, ye)).pgs)
        aux(Rect(pgser.pg(xs, ys), pgser.pg(xl, yl)).pgs)
    }else {
        lay(Rect(pgser.pg(0, ys), pgser.pg(xe, yl)).pgs)
        aux(Rect(pgser.pg(xs, 0), pgser.pg(xl, ye)).pgs)
    }
}


fun main(args: Array<String>) {
    play(squadVersusCorner)
}