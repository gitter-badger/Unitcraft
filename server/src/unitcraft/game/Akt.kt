package unitcraft.game

import org.json.simple.JSONAware
import java.util.HashMap
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import unitcraft.server.Violation
import kotlin.properties.Delegates

class Akt(val pgAim:Pg,val tile:Int,val fnAkt: (() -> Unit)?,val opter: Opter?) : JSONAware{

    init{
        if(fnAkt==null && opter==null) throw Err("fnAkt and opter == null")
    }

    fun akt() = (fnAkt?:throw Violation("akt is opter"))()
    fun aktOpt(num:Int){
        if(opter==null) throw Violation("akt is simple")
        if(num<opter.opts.size()) opter.opts[num].fn() else throw Violation("idx of opter out of bound")
    }

    override fun toJSONString() = jsonObj {
        put("x", pgAim.x)
        put("y", pgAim.y)
        put("dab", DabTile(tile))
        put("opter", opter)
    }
}

// окно выбора
class Opter(val opts : List<Opt>) : JSONAware{
    override fun toJSONString() = opts.toJSONString()
}

// что рисовать и тип указывающий на выбор
class Opt(val dabs:List<Dab>,val fn:()->Unit): JSONAware{
    override fun toJSONString() = dabs.toJSONString()
}

class Sloy(var isOn:Boolean) : JSONAware {
    val akts = ArrayList<Akt>()

    fun aktByPg(pg: Pg) = akts.firstOrNull { it.pgAim == pg }

    override fun toJSONString() = jsonObj {
        set("akts", akts)
        set("isOn", isOn)
    }
}
