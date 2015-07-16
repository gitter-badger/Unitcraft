package unitcraft.game.rule

import unitcraft.game.Drawer
import unitcraft.game.PriorDraw
import unitcraft.game.Resource
import unitcraft.game.Mover
import java.util.*

class Lifer(r: Resource,drawer: Drawer,val mover: Mover) {
    private val hintTextLife = r.hintTextLife

    fun heal(obj:Obj,value:Int){
        obj.life += value
    }

    fun damage(obj:Obj,value:Int){
        obj.life -= value
    }

    init{
        drawer.drawObjs.add {obj,side,ctx ->
            ctx.drawText(obj.head(), obj.life, hintTextLife)
        }
    }
}