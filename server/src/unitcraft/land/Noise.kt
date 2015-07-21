package unitcraft.land

import unitcraft.game.Pg
import java.util.HashMap
import unitcraft.server.init

class Noise(land:Land){
    val values = HashMap<Pg, Double>().init{
        for(pg in land.pgser) this[pg] = land.r.nextDouble()
    }
    fun get(pg:Pg) = values[pg]
}