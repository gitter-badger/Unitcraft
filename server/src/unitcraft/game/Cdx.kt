package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.game.rule.MsgRaiseA
import unitcraft.game.rule.MsgRaiseVoin
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
//    val optsTest = ArrayList<Opt>()

//    fun extend(cdx:Cdx,land:Land,g:Game):Rules{
//        val extRules = cdx.initRules(land,g)
//        rules.addAll(extRules)
//        return extRules
//    }

    fun draw(prior: Int, apply: CtxDraw.() -> Unit) {
        rules.add(RuleDraw(prior, apply))
    }

//    fun spot(prior: Int, apply: CtxSpot.() -> Unit) {
//        rules.add(RuleSpot(prior, apply))
//    }

    fun edit(prior: Int, tile: Int, apply: CtxEdit.() -> Unit) {
        rules.add(RuleEdit(prior, tile, apply))
    }

    fun endTurn(prior: Int, apply: () -> Unit) {
        rules.add(RuleEndTurn(prior, apply))
    }

    fun info(prior: Int, apply: CtxInfo.() -> Unit) {
        rules.add(RuleInfo(prior, apply))
    }

    fun stop(prior: Int, apply: CtxStop.() -> Unit) {
        rules.add(RuleStop(prior, apply))
    }

    fun make(prior: Int, apply: CtxMake.() -> Unit) {
        rules.add(RuleMake(prior, apply))
    }

    fun after(prior: Int, apply: CtxMake.() -> Unit) {
        rules.add(RuleAfter(prior, apply))
    }
}

abstract class Rule(val prior: Int)

class RuleDraw(prior: Int, val apply: (CtxDraw) -> Unit) : Rule(prior)
class CtxDraw(val side: Side) {
    val dabOnGrids = ArrayList<DabOnGrid>()

    fun drawTile(pg: Pg, tile: Int, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabTile(tile, hint)))
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        dabOnGrids.add(DabOnGrid(pg, DabText(text, hint)))
    }
}

class RuleEndTurn(prior: Int, val apply: () -> Unit) : Rule(prior)

class RuleSpot(prior: Int, val apply: (CtxSpot) -> Unit) : Rule(prior)
class CtxSpot(private val g:Game,val pgRaise: Pg, val side: Side) {
    val raises = ArrayList<Raise>()

    fun raise(msg: MsgRaise):Raise?{
        val r = Raise(g, if (g.sideTurn == side) g.info(msg).isOn else false)
        raises.add(r)
        return r
    }

    fun sloys():List<Sloy>{
        // схлопнуть
        return raises.flatMap{it.sloys()}
    }
}

class Raise(private val g:Game,val isOn:Boolean){
    private val listSloy = ArrayList<Sloy>()

    fun add(pgAkt:Pg, tlsAkt: TlsAkt, efk: Efk) {
        if(!g.stop(efk)) addAkt(Akt(pgAkt, tlsAkt(isOn), efk, null))
    }
//    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // заполнить пустоты сверху снизу
        return listSloy
    }
}

class RuleInfo(prior: Int, val apply: (CtxInfo) -> Unit) : Rule(prior)
class CtxInfo(val msg:Msg)

class RuleStop(prior: Int, val apply: (CtxStop) -> Unit) : Rule(prior)
class CtxStop(val msg:Msg)

class RuleMake(prior: Int, val apply: (CtxMake) -> Unit) : Rule(prior)
class CtxMake(val msg:Msg)

class RuleAfter(prior: Int, val apply: (CtxMake) -> Unit) : Rule(prior)

class RuleEdit(prior: Int, val tile: Int, val apply: (CtxEdit) -> Unit) : Rule(prior)
class CtxEdit(val efk: EfkEdit) {
    var isConsumed = false
    fun consume(b: Boolean) {
        isConsumed = b
    }
}

abstract class Msg{
    var isStoped:Boolean = false
        private set

    fun stop(){
        isStoped = true
    }

    fun refute(){
        isStoped = false
    }
}

abstract class Efk : Msg()

abstract class EfkEdit(val pg:Pg):Efk()
class EfkEditAdd(pg:Pg,val side:Side):EfkEdit(pg)
class EfkEditRemove(pg:Pg):EfkEdit(pg)
class EfkEditDestroy(pg:Pg):EfkEdit(pg)
class EfkEditChange(pg:Pg,val side:Side):EfkEdit(pg)

