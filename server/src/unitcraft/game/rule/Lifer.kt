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

    fun life(obj:Obj) = obj.getOrPut(life){Life(5)} as Life

    fun heal(obj:Obj,value:Int){
        life(obj).alter(value)
    }

    fun damage(obj:Obj,value:Int){
        life(obj).alter(-value)
    }

    init{
        drawer.draws.add {obj,side,ctx ->
            ctx.drawText(obj.shape.head, life(obj).value, hintTextLife)
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
