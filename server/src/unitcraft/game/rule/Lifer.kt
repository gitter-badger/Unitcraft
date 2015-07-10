package unitcraft.game.rule

import unitcraft.game.Drawer
import unitcraft.game.PriorDraw
import unitcraft.game.Resource
import unitcraft.game.Shaper
import java.util.*

class Lifer(r: Resource,drawer: DrawerVoin,val shaper: Shaper) {
    private val life = "life"
    private val hintTextLife = r.hintTextLife

    val kinds = ArrayList<Kind>()

    fun life(obj:Obj) = obj[life] as Life

    init{
        drawer.draws.add {obj,side,ctx ->
            ctx.drawText(obj.shape.head, obj<Life>().value, hintTextLife)
        }
        shaper.creates.add{obj ->
            obj[life] = Life(5)
        }
    }
}


class Life(valueInit: Int) {
    var value: Int = valueInit
        private set

    fun alter(d: Int) {
        value += d
    }

    override fun toString() = "Life($value)"
}
