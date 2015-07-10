package unitcraft.game.rule

import unitcraft.game.Resource
import java.util.*

class Lifer(r: Resource,drawer:DrawerVoin) {
    private val life = "life"
    private val hintTextLife = r.hintTextLife

    val kinds = ArrayList<Kind>()

    fun get(obj: Obj, prop: PropertyMetadata): Life {
        return obj.getOrPut(prop.name){Life(5)} as Life
    }

    fun set(obj: Obj, prop: PropertyMetadata, v: Life) {
        obj[prop.name] = v
    }

    init{
        drawer.draws.add{ obj,side,ctx ->
            if(obj)
            ctx.drawText(shape.head, obj[life].value, hintTextLife)
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
