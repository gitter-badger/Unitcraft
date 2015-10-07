package unitcraft.land

import unitcraft.game.Pg
import java.util.HashMap

class Noise(land:Land){
    val values = HashMap<Pg, Double>().apply{
        for(pg in land.pgser) this[pg] = land.r.nextDouble()
    }
    fun get(pg:Pg) = values[pg]
}