package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.PriorDraw
import unitcraft.server.Side
import java.util.*

open class Obj(var kind:Kind,val priorDraw: PriorDraw,var shape:Shape){
    private val props = HashMap<String, Any>()

    fun get(key:String):Any? = props[key]
    fun set(key:String,value:Any){ props[key] = value }
    fun remove(key:String) = props.remove(key)
    fun getOrPut(key:String,def:()->Any) = props.getOrPut(key,def)

    override fun toString(): String {
        return "Obj "+props
    }
}

open class ObjOwn(kind:Kind,priorDraw: PriorDraw,shape:Shape):Obj(kind,priorDraw,shape){
    var side = Side.n
}

class Voin(kind:Kind,priorDraw: PriorDraw,shape: Shape,hider:Hider,enforcer: Enforcer,lifer:Lifer):ObjOwn(kind,priorDraw,shape){
    var enforced by enforcer
    var hided by hider
    var flip = false
    var life by lifer
}

class Objs:MutableList<Obj> by ArrayList<Obj>()

fun List<Obj>.byKind(kind:Kind) = filter { it.kind == kind }

fun <T:Obj> List<T>.byPg(pg: Pg) = filter { (it.shape as? Singl)?.pg == pg }

abstract class Shape

class Singl(val pg:Pg) : Shape()