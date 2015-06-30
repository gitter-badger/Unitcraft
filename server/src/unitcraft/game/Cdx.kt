package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlin

abstract class Cdx(val r: Resource) {
    abstract fun createRules(land: Land, g: Game): Rules
    fun rules(fn: Rules.() -> Unit):Rules{
        val rules = Rules()
        rules.fn()
        return rules
    }
}

open class Rules() {
    val rulesInfo = HashMap<KClass<out Msg>,MutableList<Rule>>()
    val rulesStop = HashMap<KClass<out Efk>,MutableList<Rule>>()
    val rulesMake = HashMap<KClass<out Efk>,MutableList<Rule>>()
    val rulesAfter = HashMap<KClass<out Efk>,MutableList<Rule>>()

    inline fun <reified T:Msg> info(prior: Int, @noinline apply: T.() -> Unit) {
        rulesInfo.getOrPut(javaClass<T>().kotlin){ArrayList<Rule>()}.add(Rule(prior,apply))
    }

    inline fun <reified T:Efk> stop(prior: Int, @noinline apply: T.() -> Unit) {
        rulesStop.getOrPut(javaClass<T>().kotlin){ArrayList<Rule>()}.add(Rule(prior,apply))
    }

    inline fun <reified T:Efk> after(prior: Int, @noinline apply: T.() -> Unit) {
        rulesAfter.getOrPut(javaClass<T>().kotlin){ArrayList<Rule>()}.add(Rule(prior,apply))
    }

    inline fun <reified T:Efk> make(prior:Int, @noinline apply: T.() -> Unit){
        rulesMake.getOrPut(javaClass<T>().kotlin){ArrayList<Rule>()}.add(Rule(prior,apply))
    }

    fun endTurn(prior:Int,apply: EfkEndTurn.() -> Unit){
        after(prior,apply)
    }

    val tilesEditAdd = ArrayList<Pair<Int,Int>>()
    fun editAdd(prior:Int,tile: Int,apply: EfkEditAdd.() -> Unit){
        tilesEditAdd.add(prior to tile)
        make(prior,apply)
    }

    fun addRules(r:Rules):Rules{
        addRulesTo(rulesInfo,r.rulesInfo)
        addRulesTo(rulesStop,r.rulesStop)
        addRulesTo(rulesMake,r.rulesMake)
        addRulesTo(rulesAfter,r.rulesAfter)
        tilesEditAdd.addAll(r.tilesEditAdd)
        return this
    }

    companion object {
        fun <K> addRulesTo(mapTo: HashMap<K, MutableList<Rule>>, map: Map<K, List<Rule>>) {
            for ((key, list) in map) {
                mapTo.getOrPut(key) { ArrayList<Rule>() }.addAll(list)
            }
        }
    }
}

class Rule(val prior:Int,appl: Any){
    val apply = appl as (Any)->Unit
}

class MsgDraw(val sideVid: Side):Msg() {
    val dabOnGrids = ArrayList<DabOnGrid>()

    fun drawTile(pg: Pg, tile: Int, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabTile(tile, hint)))
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabText(text, hint)))
    }
}

abstract class Msg

abstract class Efk{
    var isEated:Boolean = false
        private set

    var isStoped:Boolean = false
        private set

    fun stop(){
        isStoped = true
    }

    fun eat(){
        isEated = true
    }

    fun refute(){
        isStoped = false
    }
}

abstract class EfkEdit(val pgEdit:Pg) : Efk()
class EfkEditAdd(pg:Pg,val sideVid:Side):EfkEdit(pg)
class EfkEditRemove(pg:Pg):EfkEdit(pg)
class EfkEditDestroy(pg:Pg):EfkEdit(pg)
class EfkEditChange(pg:Pg,val sideVid:Side):EfkEdit(pg)

