package unitcraft.lander

import unitcraft.game.Pgser
import unitcraft.game.rule.Flater
import unitcraft.game.rule.Objer
import unitcraft.inject.inject

class Lander {
    val objer: Objer by inject()
    val flater: Flater by inject()

    fun land(mission: Int?): Land {
        val seed = mission?.toLong() ?: System.nanoTime()
        val random = Random(seed)
        return spotsAndRiver(random, createPgser(random), flater.maxFromTpFlat, objer.maxFromTpSolid)
    }

    private fun createPgser(random: Random): Pgser {
        val (x, y) = dmns[random.nextInt(dmns.size)]
        return Pgser(x, y)
    }

    val dmns = listOf(
            13 to 11, 13 to 10, 13 to 9,
            12 to 11, 12 to 10, 12 to 9,
            11 to 11, 11 to 10, 11 to 9,
            10 to 10, 10 to 9,
            9 to 9
    )
}