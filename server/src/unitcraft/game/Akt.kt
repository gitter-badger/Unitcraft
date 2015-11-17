package unitcraft.game

import org.json.simple.JSONAware
import org.json.simple.JSONObject
import java.util.HashMap
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.ArrayList
import unitcraft.server.Violation
import kotlin.properties.Delegates

//class Akt(val pgAim:Pg,val tile:Int, val fn:(()->Unit)?=null,val opter: Opter?,val fnOpt:(()->Unit)?=null) : JSONAware{
//    override fun toJSONString() = jsonObj {
//        put("x", pgAim.x)
//        put("y", pgAim.y)
//        put("dab", DabTile(tile))
//        if(opter!=null) put("opter", opter)
//    }
//}

abstract class Akt(val pg:Pg,val tlsAkt: TlsAkt)

class AktSimple(pg:Pg,tlsAkt: TlsAkt, val fn: () -> Unit):Akt(pg,tlsAkt)

class AktOpt(pg:Pg,tlsAkt: TlsAkt,val dabs:List<List<Dab>>, val fn: (Int) -> Unit):Akt(pg,tlsAkt)

// окно выбора
class Opter(val opts : List<Opt>) : JSONAware{
    override fun toJSONString() = opts.toJSONString()
}

// что рисовать и что вызвать
class Opt(val dabs:List<Dab>): JSONAware{

    constructor(dab:Dab):this(listOf(dab))

    override fun toJSONString() = dabs.toJSONString()
}

class Sloy(var isOn:Boolean,val hintTileAktOff:HintTile) : JSONAware {
    val akts = ArrayList<Akt>()

    fun aktByPg(pg: Pg) = akts.firstOrNull { it.pg == pg }

    override fun toJSONString() = jsonObj {
        set("akts", akts.map{jsonAkt(it,isOn)})
        set("isOn", isOn)
    }

    fun jsonAkt(akt:Akt,isOn:Boolean) = JSONObject().apply {
        put("x", akt.pg.x)
        put("y", akt.pg.y)
        put("dab", DabTile(akt.tlsAkt(),if(isOn) null else hintTileAktOff))
        if(akt is AktOpt) put("opter", Opter(akt.dabs.map{Opt(it)}))
    }
}
