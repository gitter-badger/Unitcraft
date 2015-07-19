package unitcraft.game.rule

import unitcraft.game.*
import java.util.*

class Lifer(r: Resource,drawer: Drawer,val objs: ()->Objs) {
    private val hintTextLife = r.hintText("ctx.fillStyle = 'white';")

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

    init{
        drawer.drawObjs.add {obj,side,ctx ->
            ctx.drawText(obj.head(), obj.life, hintTextLife)
        }
    }
}