package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.ArrayList

abstract class Cdx(val r: Resource) {
    abstract fun createRules(land: Land, g: Game): List<Rule>
}

fun rules(fn: Rules.() -> Unit): List<Rule> {
    val rules = Rules()
    rules.fn()
    return rules.rules
}

class Rules {
    val rules = ArrayList<Rule>()

    fun edit(prior: Int, tile: Int, apply: CtxEdit.() -> Unit) {
        rules.add(RuleEdit(prior, tile, apply))
    }

    fun endTurn(prior: Int, apply: () -> Unit) {
        rules.add(RuleEndTurn(prior, apply))
    }

    fun info(prior: Int, apply: CtxInfo.() -> Unit) {
        rules.add(RuleInfo(prior, apply))
    }

    fun make(prior: Int, apply: CtxMake.() -> Unit) {
        rules.add(RuleMake(prior, apply))
    }

    fun stop(prior: Int, apply: CtxMake.() -> Unit) {
        rules.add(RuleStop(prior, apply))
    }

    fun after(prior: Int, apply: CtxMake.() -> Unit) {
        rules.add(RuleAfter(prior, apply))
    }
}

abstract class Rule(val prior: Int)

class MsgDraw(val side: Side):Msg() {
    val dabOnGrids = ArrayList<DabOnGrid>()

    fun drawTile(pg: Pg, tile: Int, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabTile(tile, hint)))
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabText(text, hint)))
    }
}

class RuleEndTurn(prior: Int, val apply: () -> Unit) : Rule(prior)

class RuleInfo(prior: Int, val apply: (CtxInfo) -> Unit) : Rule(prior)
class CtxInfo(val msg:Msg)

class RuleStop(prior: Int, val apply: (CtxMake) -> Unit) : Rule(prior)

class RuleMake(prior: Int, val apply: (CtxMake) -> Unit) : Rule(prior)
class CtxMake(val efk:Efk)

class RuleAfter(prior: Int, val apply: (CtxMake) -> Unit) : Rule(prior)

class RuleEdit(prior: Int, val tile: Int, val apply: (CtxEdit) -> Unit) : Rule(prior)
class CtxEdit(val efk: EfkEdit)

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

abstract class EfkEdit(val pg:Pg) : Efk()
class EfkEditAdd(pg:Pg,val side:Side):EfkEdit(pg)
class EfkEditRemove(pg:Pg):EfkEdit(pg)
class EfkEditDestroy(pg:Pg):EfkEdit(pg)
class EfkEditChange(pg:Pg,val side:Side):EfkEdit(pg)

