package unitcraft.lander

import unitcraft.lander.TpFlat.*

val spotsAndRiver = land{
    for (pg in pgser) {
        addFlat(pg, none, 0)
    }
    repeat(rnd(0..2)) { lay(spot(rnd(5..15)),wild) }
    if(random.nextBoolean()) lay(river,liquid)
    layDistinct(spray(rnd(0..10)), special)
    layFormation(formationChess(10,3))
}

fun main(args: Array<String>) {
    play(spotsAndRiver)
}