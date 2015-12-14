package unitcraft.lander

import unitcraft.lander.TpFlat.*

val spotsAndRiver = land{
    for (pg in pgser) {
        addFlat(pg, none, 0)
    }
    repeat(rnd(0..2)) { lay(spot(rnd(5..15)),wild) }
    if(random.nextBoolean()) lay(river,liquid)
    layDistinct(spray(rnd(1..10)), special)
    squad(rnd(squads)!!)
}

val squads = listOf(squadVersus(3),squadVersusCorner,squadChess,squadSpray(5))

fun main(args: Array<String>) {
    play(spotsAndRiver)
}