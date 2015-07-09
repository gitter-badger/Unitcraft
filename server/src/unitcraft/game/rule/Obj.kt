package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.game.PriorDraw
import unitcraft.server.Side
import java.util.*

open class Obj(var kind:Kind,var shape:Shape){
    private val props = HashMap<String, Any>()

    fun get(key:String):Any? = props[key]
    fun set(key:String,value:Any){ props[key] = value }
    fun remove(key:String) = props.remove(key)
    fun getOrPut(key:String,def:()->Any) = props.getOrPut(key,def)

    override fun toString(): String {
        return "Obj "+props
    }
}

open class ObjOwn(kind:Kind,shape:Shape):Obj(kind,shape){
    var side = Side.n
}

open class Voin(kind:Kind,shape: Shape,hider:Hider,enforcer: Enforcer,lifer:Lifer):ObjOwn(kind,shape){
    var enforced by enforcer
    var hided by hider
    var life by lifer
    var tired = true
}

class VoinFuel(kind:Kind,shape: Shape,hider:Hider,enforcer: Enforcer,lifer:Lifer):
        Voin(kind,shape,hider,enforcer,lifer){
    var fuel = 3
}

class Objs:MutableList<Obj> by ArrayList<Obj>()

fun List<Obj>.byKind(kind:Kind) = filter { it.kind == kind }
fun List<Obj>.byKind(kinds:List<Kind>) = filter { it.kind in kinds }

fun <T:Obj> List<T>.byPg(pg: Pg) = filter { (it.shape as? Singl)?.pg == pg }

abstract class Shape

class Singl(val pg:Pg) : Shape(){
    var flip = false
}

abstract class Kind{
    fun name() = this.javaClass.getSimpleName()
}