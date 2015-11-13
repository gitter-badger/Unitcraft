package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import java.util.*

class Lifer(r: Resource) {
    private val hintTextLife = r.hintText("ctx.fillStyle = 'white';")

    val objs: ()->Objs by injectObjs()

    fun heal(obj:Obj,value:Int){
        obj.life += value
    }

    fun damage(obj:Obj,value:Int){
        obj.life -= value
        if(obj.life<=0) objs().remove(obj)
    }

    fun damage(pg:Pg,value:Int){
        objs()[pg]?.let{
            damage(it,value)
        }
    }

    fun canDamage(pg: Pg):Boolean{
        return objs()[pg]!=null
    }

    fun change(obj:Obj, value:Int){
        obj.life = value
    }

    init{
        injectValue<Drawer>().drawObjs.add {obj,side,ctx ->
            ctx.drawText(obj.head(), obj.life, hintTextLife)
        }
    }
}