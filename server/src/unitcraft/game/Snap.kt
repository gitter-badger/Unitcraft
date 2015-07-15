package unitcraft.game

import org.json.simple.JSONObject
import org.json.simple.JSONAware
import org.json.simple.JSONValue
import unitcraft.server.init
import java.util.*

// текущее состояние игры, выраженное инструкциями по отрисовке для клиента
class Snap(
        val xr: Int,
        val yr: Int,
        val grid: List<DabOnGrid>,
        val spots: Map<Pg, List<Sloy>>,
        val traces: List<DabOnGrid>,
        val canEndTurn: Boolean,
        val stage: Stage,
        val opterTest:Opter?
) {

    fun toJson() = JSONObject().init {
        put("dmn",JSONObject().init {
            put("xr",xr)
            put("yr",yr)
        })
        put("grid",grid)
        put("spots",spots)
        put("traces", traces)
        put("edge", if(canEndTurn) 0 else 1)
        put("canEndTurn", canEndTurn)
        put("vpoint",listOf(15,7))
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
class DabTile(val tile:Tile,val hint:Int? = null) : Dab() {
    override fun toJSONString() = jsonObj {
        set("tile", tile)
        if(hint!=null) set("hint", hint)
    }
}

// какой текст и как рисовать
class DabText(val text:String,val hint:Int? = null) : Dab() {
    override fun toJSONString() = jsonObj {
        put("text", text)
        if(hint!=null) set("hint", hint)
    }
}

// команда нарисовать
abstract class Dab : JSONAware

enum class Stage{
    bonus, bonusEnemy, turn, turnEnemy, win, winEnemy
}

fun jsonObj(init:JSONObject.()->Unit):String{
    val obj = JSONObject()
    obj.init()
    return obj.toJSONString()!!
}

fun List<JSONAware>.toJSONString() = JSONValue.toJSONString(this)
fun Map<JSONAware,JSONAware>.toJSONString() = JSONValue.toJSONString(this)