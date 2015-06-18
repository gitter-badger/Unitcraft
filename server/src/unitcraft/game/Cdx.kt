package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.ArrayList

abstract class Cdx(r: Resource) {
    abstract fun initRules(land: Land, g: Game): List<Rule>
}

fun rules(fn: Rules.() -> Unit): List<Rule> {
    val rules = Rules()
    rules.fn()
    return rules.rules
}

class Rules {
    val rules = ArrayList<Rule>()
    val optsTest = ArrayList<Opt>()

    fun draw(prior: Int, apply: CtxDraw.() -> Unit) {
        rules.add(RuleDraw(prior, apply))
    }

    fun spot(prior: Int, apply: CtxSpot.() -> Unit) {
        rules.add(RuleSpot(prior, apply))
    }

    fun tgglRaise(prior: Int, apply: CtxTgglRaise.() -> Unit) {
        rules.add(RuleTgglRaise(prior, apply))
    }

    fun edit(prior: Int, tile: Int, apply: CtxEdit.() -> Unit) {
        rules.add(RuleEdit(prior, tile, apply))
    }

    fun voin(prior: Int, apply: CtxVoin.() -> Unit) {
        rules.add(RuleVoin(prior, apply))
    }

    fun endTurn(prior: Int, apply: () -> Unit) {
        rules.add(RuleEndTurn(prior, apply))
    }

    fun make(prior: Int, apply: CtxMake.() -> Unit) {
        rules.add(RuleMake(prior, apply))
    }

    fun stop(prior: Int, apply: CtxStop.() -> Boolean) {
        rules.add(RuleStop(prior, apply))
    }
// может refute следует через make и stop? или выделение refute улучшает семантику?
//    fun refute(prior: Int, apply: (CtxRefute) -> Boolean) {
//
//    }
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

class RuleSpot(prior: Int, val apply: (CtxSpot) -> Unit) : Rule(prior)
class CtxSpot(val pgRaise: Pg, val side: Side, private val g:Game) {
    val raises = ArrayList<Raise>()

    fun raise(sideOwner:Side?):Raise{
        val ctxTggl = CtxTgglRaise(pgRaise,side,g.sideTurn==side && g.sideTurn==sideOwner)
        g.rulesTgglRaise.forEach{it.apply(ctxTggl)}
        val r = Raise(sideOwner,ctxTggl.isOn)
        raises.add(r)
        return r
    }

    fun sloys():List<Sloy>{
        // схлопнуть
        return raises.flatMap{it.sloys()}
    }
}

class Raise(val sideOwner:Side?,val isOn:Boolean){
    private val listSloy = ArrayList<Sloy>()

    fun akt(pgAim: Pg, tlsAkt: TlsAkt, fnAkt: () -> Unit) = addAkt(Akt(pgAim, tlsAkt(isOn), fnAkt, null))
    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
        if (idx == listSloy.size()) listSloy.add(Sloy())
        listSloy[idx].akts.add(akt)
    }

    fun isNotEmpty() = listSloy.isNotEmpty()

    fun sloys(): List<Sloy> {
        // заполнить пустоты сверху снизу
        return listSloy
    }
}

class RuleTgglRaise(prior: Int, val apply: (CtxTgglRaise) -> Unit) : Rule(prior)
class CtxTgglRaise(val pgRaise: Pg, val side: Side,var isOn:Boolean){
    fun isOn(b:Boolean){
        isOn = b
    }
}

class RuleStop(prior: Int, val apply: (CtxStop) -> Boolean) : Rule(prior)
class CtxStop(val from: From, val aim: Aim, val tp: TpMake)

class RuleMake(prior: Int, val apply: (CtxMake) -> Unit) : Rule(prior)
class CtxMake(val from: From, val aim: Aim, val tp: TpMake)

enum class TpMake {
    move, // пойти из from.pg в aim.pg
    skil, // применить способность
    hide // скрыть точку aim.pg
}

class RuleEdit(prior: Int, val tile: Int, val apply: (CtxEdit) -> Unit) : Rule(prior)
class CtxEdit(val tp: TpEdit, val pgAim: Pg, val side: Side) {
    var isConsumed = false
    fun consume(b: Boolean) {
        isConsumed = b
    }
}

enum class TpEdit {
    add, remove, destroy, change
}

class RuleVoin(prior: Int, val apply: (CtxVoin) -> Unit) : Rule(prior)
class CtxVoin(val pg: Pg, val side: Side) {
    var voin: Voin? = null
        private set

    fun put(voin: Voin) {
        this.voin = voin
    }
}

class RuleEndTurn(prior: Int, val apply: () -> Unit) : Rule(prior)