import unitcraft.server.Err
import unitcraft.server.Side

fun sideAfterChange(sideObj: Side, sideVid: Side) = when {
    sideObj == Side.n -> sideVid
    sideObj == sideVid -> sideVid.vs
    sideObj != sideVid -> Side.n
    else -> throw Err("assertion")
}
