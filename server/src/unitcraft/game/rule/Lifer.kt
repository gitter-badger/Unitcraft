package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.server.Err
import java.util.*

class Lifer(r: Resource) {
    val slotStopDamage = ArrayList<(Obj)->Boolean>()
    val slotAfterDamage = ArrayList<(List<Dmg>)->Unit>()
    val slotAfterDeaths = ArrayList<(List<Obj>)->Unit>()

    val allData by injectAllData()

    fun objs() = allData().objs

    fun corpses() = allData().corpses

    fun heal(obj:Obj,value:Int){
        obj.life += value
    }

    fun damage(dmgs:List<Dmg>){
        dmgs.forEach {
            if(canDamage(it.obj)) it.obj.life -= it.value
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

    fun canDamage(pg: Pg) = objs()[pg]?.let{canDamage(it)}?:false

    fun canDamage(obj:Obj) = !slotStopDamage.any{it(obj)}

    fun change(obj:Obj, value:Int){
        obj.life = value
    }

    private fun funeral(){
        while(true) {
            val deads = objs().filter { it.life <= 0 }
            if(deads.isEmpty()) break
            slotAfterDeaths.forEach { it(deads) }
            objs().list.removeAll(deads)
            deads.forEach {
                corpses().replace(it)
            }
        }
        objs().list.removeIf { it.life<=0 }
    }

    init{
        val hintTextLife = r.hintText("ctx.fillStyle = 'white';")
        injectValue<Objer>().slotDrawObjPost.add(100,this,"рисует прочность") {
            ctx.drawText(obj.pg, obj.life, hintTextLife)
        }
    }
}

data class Dmg(val obj:Obj,val value:Int){
    init{
        if(value==0) throw Err("dmg != 0")
    }
}