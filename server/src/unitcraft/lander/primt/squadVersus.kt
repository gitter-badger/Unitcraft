package unitcraft.lander

fun squadVersus(distEdge: Int) = primt {
    val isVert = rndBln()
    lay((if(isVert) Rect(pgser.pg(0,0),pgser.pg(distEdge-1,yl)) else Rect(pgser.pg(0,0),pgser.pg(xl,distEdge-1))).pgs)
    aux((if (isVert) Rect(pgser.pg(xr-distEdge,0),pgser.pg(xl,yl)) else Rect(pgser.pg(0,yr-distEdge),pgser.pg(xl,yl))).pgs)
}


fun main(args: Array<String>) {
    play(squadVersus(3))
}