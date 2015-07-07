package unitcraft.game

import org.json.simple.JSONAware
import java.util.HashMap
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import unitcraft.server.Violation
import kotlin.properties.Delegates

class Akt(val pgAim:Pg,val tile:Int,val opter: Opter?, val fn:(()->Unit)?=null) : JSONAware{
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

// что рисовать и что вызвать
class Opt(val dabs:List<Dab>): JSONAware{

    constructor(dab:Dab):this(listOf(dab))

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
