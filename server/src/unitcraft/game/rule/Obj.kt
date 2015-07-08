package unitcraft.game.rule

import unitcraft.game.Pg
import unitcraft.server.Side
import java.util.*

open class Obj(var kind:Kind,shaper: Shaper){

    var shape:Shape by shaper

    private val props = HashMap<String, Any>()

    fun get(key:String):Any? = props[key]
    fun set(key:String,value:Any){ props[key] = value }
    fun remove(key:String) = props.remove(key)
    fun getOrPut(key:String,def:()->Any) = props.getOrPut(key,def)

    override fun toString(): String {
        return "Obj "+props
    }
}

open class ObjOwn(kind:Kind,shaper: Shaper,sider:Sider):Obj(kind,shaper){
    var side: Side by sider
}

class Voin(kind:Kind,shaper: Shaper,sider:Sider,hider:Hider,enforcer: Enforcer,lifer:Lifer):ObjOwn(kind,shaper,sider){
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