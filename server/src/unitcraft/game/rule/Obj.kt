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

    override fun toString(): String {
        return "Obj "+props
    }
}

open class ObjOwn(kind:Kind,shaper: Shaper,sider:Sider):Obj(kind,shaper){
    var side: Side by sider
}

class Voin(kind:Kind,shaper: Shaper,sider:Sider,hider:Hider, enforcer: Enforcer):ObjOwn(kind,shaper,sider){
    var enforced: Boolean? by enforcer
    val hided:Boolean by hider
}

class Objs:Sequence<Obj>{
    val objs = ArrayList<Obj>()
    override fun iterator() = objs.iterator()
}

fun Sequence<Obj>.byKind(kind:Kind) = filter { it.kind == kind }

fun <T:Obj> Sequence<T>.byPg(pg: Pg) = filter { (it.shape as? Singl)?.pg == pg }

abstract class Shape

class Singl(val pg:Pg) : Shape()