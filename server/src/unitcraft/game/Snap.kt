package unitcraft.game

import org.json.simple.JSONObject
import org.json.simple.JSONAware
import org.json.simple.JSONValue

// текущее состояние игры, выраженное инструкциями по отрисовке для клиента
class Snap(
        val xr: Int,
        val yr: Int,
        val grid: List<DabOnGrid>,
        val spots: Map<Pg, List<Sloy>>,
        val traces: List<DabOnGrid>,
        val stage: Stage,
        val edge:DabTile,
        val dabFocus:DabTile,
        val dabFocusMore:DabTile,
        val focus:Pg?,
        val vpoint:List<Int>,
        val opterTest:Opter?
) {

    fun toJson() = JSONObject().apply {
        put("dmn",JSONObject().apply {
            put("xr",xr)
            put("yr",yr)
        })
        put("grid",grid)
        put("spots",spots)
        put("traces", traces)
        put("edge", edge)
        put("dabFocus",dabFocus)
        put("dabFocusMore",dabFocusMore)
        if(focus!=null) put("focus",JSONObject().apply {
            put("x",focus.x)
            put("y",focus.y)
        })
        put("vpoint",vpoint)
        put("stage",stage.toString())
        if(opterTest!=null) put("opterTest",opterTest)
    }
}

//  где на поле и что рисовать
class DabOnGrid(val pg:Pg,val dab: Dab): JSONAware{
    override fun toJSONString() = jsonObj {
        put("x", pg.x)
        put("y", pg.y)
        put("dab", dab)
    }
}

// какой тайл и как рисовать
class DabTile(val tile:Tile,val hint:HintTile? = null) : Dab() {
    override fun toJSONString() = jsonObj {
        set("tile", tile)
        if(hint!=null) set("hint", hint)
    }
}

// какой текст и как рисовать
class DabText(val text:String,val hint:HintText? = null) : Dab() {
    override fun toJSONString() = jsonObj {
        put("text", text)
        if(hint!=null) set("hint", hint)
    }
}

// команда нарисовать
abstract class Dab : JSONAware

fun jsonObj(init:JSONObject.()->Unit):String{
    val obj = JSONObject()
    obj.init()
    return obj.toJSONString()!!
}

fun List<JSONAware>.toJSONString() = JSONValue.toJSONString(this)
fun Map<JSONAware,JSONAware>.toJSONString() = JSONValue.toJSONString(this)