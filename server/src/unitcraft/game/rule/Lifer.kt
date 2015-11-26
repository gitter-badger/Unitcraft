package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Err
import java.util.*

class Lifer(r: Resource) {
    val slotAfterDamage = ArrayList<(List<Dmg>)->Unit>()

    val objs: ()->Objs by injectObjs()

    fun heal(obj:Obj,value:Int){
        obj.life += value
    }

    fun damage(dmgs:List<Dmg>){
        dmgs.forEach {
            it.obj.life -= it.value
        }
        slotAfterDamage.forEach { it(dmgs) }
        funeral()
    }

    fun damage(aims:List<Obj>,value:Int){
        damage(aims.map{Dmg(it,value)})
    }

    fun damage(obj:Obj,value:Int){
        damage(listOf(Dmg(obj,value)))
    }

    fun damage(pg:Pg,value:Int){
        objs()[pg]?.let{
            damage(it,value)
        }
    }

    fun canDamage(pg: Pg):Boolean{
        return objs()[pg]!=null
    }

    fun canDamage(obj:Obj):Boolean{
        return true
    }

    fun change(obj:Obj, value:Int){
        obj.life = value
    }

    private fun funeral(){
        objs().forEach { if(it.life<=0) objs().remove(it) }
    }

    init{
        val hintTextLife = r.hintText("ctx.fillStyle = 'white';")
        injectValue<Drawer>().drawObjs.add {obj,side,ctx ->
            ctx.drawText(obj.pg, obj.life, hintTextLife)
        }
    }
}

data class Dmg(val obj:Obj,val value:Int){
    init{
        if(value==0) throw Err("dmg != 0")
    }
}