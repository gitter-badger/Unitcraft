package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.Resource
import java.util.*

class Magic{
    val slotStopMagic = ArrayList<(Pg)->Boolean>()

    fun canMagic(pg:Pg)= !slotStopMagic.any{it(pg)}
}
