package unitcraft.lander

val squadChess = primt {
    val xe = xr / 2 - 1
    val ye = yr / 2 - 1

    val xs = xe + 1 + xr % 2
    val ys = ye + 1 + yr % 2

    lay(Rect(pgser.pg(0, 0), pgser.pg(xe, ye)).pgs)
    lay(Rect(pgser.pg(xs, ys), pgser.pg(xl, yl)).pgs)

    aux(Rect(pgser.pg(xs, 0), pgser.pg(xl, ye)).pgs)
    aux(Rect(pgser.pg(0, ys), pgser.pg(xe, yl)).pgs)
}

fun main(args: Array<String>) {
    play(squadChess)
}