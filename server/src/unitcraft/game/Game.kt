package unitcraft.game

import unitcraft.land.Land
import unitcraft.server.GameState
import unitcraft.server.IGame
import unitcraft.server.Side
import unitcraft.server.Violation
import java.util.HashMap

class Game(cdxs: List<Cdx>, land: Land, val canEdit: Boolean = false) : IGame {
    val pgser = land.pgser
    val pgs = pgser.pgs
    var sideTurn = Side.a
        private set
    val bonus = HashMap<Side, Int>()

    val rulesDraw: List<RuleDraw>
    val rulesSpot: List<RuleSpot>
    val rulesTgglRaise: List<RuleTgglRaise>
    val rulesVoin: List<RuleVoin>
    val rulesEndTurn: List<RuleEndTurn>
    val rulesMake: List<RuleMake>
    val rulesStop: List<RuleStop>

    val opterTest: Opter?
    val rulesEdit: List<RuleEdit>?

    init {
        val rules = cdxs.flatMap { it.createRules(land, this) }
        rulesDraw = filterRules<RuleDraw>(rules)
        rulesSpot = filterRules<RuleSpot>(rules)
        rulesVoin = filterRules<RuleVoin>(rules)
        rulesEndTurn = filterRules<RuleEndTurn>(rules)
        rulesMake = filterRules<RuleMake>(rules)
        rulesStop = filterRules<RuleStop>(rules)
        rulesTgglRaise = filterRules<RuleTgglRaise>(rules)

        if (canEdit) {
            rulesEdit = filterRules<RuleEdit>(rules)
            opterTest = Opter(rulesEdit.map { Opt(listOf(DabTile(it.tile))) {} })
        } else {
            rulesEdit = null
            opterTest = null
        }
    }

    private inline fun <reified T : Rule> filterRules(rules: List<Rule>): List<T> {
        return rules.filterIsInstance<T>().sortBy { it.prior }
    }

    val traces = Traces()

    override fun cmd(side: Side, cmd: String) {
        if (cmd.isEmpty()) throw Violation("cmd is empty")
        val prm = Prm(pgser, cmd[1, cmd.length()].toString())
        when (cmd[0]) {
            'z' -> editAdd(side, prm)
            'r' -> editRemove(side, prm)
            'd' -> editDestroy(side, prm)
            'c' -> editChange(side, prm)
            'a' -> akt(side, prm)
            'b' -> aktOpt(side, prm)
            'e' -> endTurn(side, prm)
            else -> throw Violation("unknown msg: " + cmd)
        }
    }

    override fun state(): GameState {
        return GameState(null, Side.values().map { it to snap(it).toJson() }.toMap(), null)
    }

    override fun cmdRobot(): String? {
        return if (sideTurn == Side.b) "e" else null
    }

    override fun land(): String {
        throw UnsupportedOperationException()
    }

    private fun editAdd(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(3)
        val ctx = CtxEdit(TpEdit.add, prm.pg(0), side)
        rulesEdit!!.elementAtOrElse(prm.int(2)) { throw Violation("editAdd out bound") }.apply(ctx)
    }

    private fun editRemove(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        for (it in rulesEdit!!.reverse()) {
            val ctx = CtxEdit(TpEdit.remove, prm.pg(0), side)
            it.apply(ctx)
            if (ctx.isConsumed) break
        }
    }

    private fun editDestroy(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        for (it in rulesEdit!!) {
            val ctx = CtxEdit(TpEdit.destroy, prm.pg(0), side)
            it.apply(ctx)
            if (ctx.isConsumed) break
        }
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        for (it in rulesEdit!!) {
            val ctx = CtxEdit(TpEdit.change, prm.pg(0), side)
            it.apply(ctx)
            if (ctx.isConsumed) break
        }
    }

    private fun akt(side: Side, prm: Prm) {
        prm.ensureSize(5)
        val sloy = spot(prm.pg(0),side)[prm.int(2)]
        if(!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3))?:throw Violation("akt not found")
        traces.clear()
        akt.akt()
        println("akt "+side+" from "+prm.pg(0) + " index "+prm.int(2)+" to "+prm.pg(3))
    }

    private fun aktOpt(side: Side, prm: Prm) {
        prm.ensureSize(6)
        val sloy = spot(prm.pg(0),side)[prm.int(2)]
        if(!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3))?:throw Violation("akt not found")
        traces.clear()
        akt.aktOpt(prm.int(5))
        println("akt "+side+" from "+prm.pg(0) + " index "+prm.int(2)+" to "+prm.pg(3)+" opt "+prm.int(5))
    }

    private fun endTurn(side: Side, prm: Prm) {
        prm.ensureSize(0)
        if (sideTurn != side) throw Violation("endTurn side($side) != sideTurn($sideTurn)")
        rulesEndTurn.forEach { it.apply() }
        sideTurn = sideTurn.vs()
    }

    fun snap(side: Side): Snap {
        val spots = HashMap<Pg, List<Sloy>>()
        for (pg in pgs) {
            val sloys = spot(pg, side)
            if (sloys.isNotEmpty()) spots[pg] = sloys
        }
        val ctxDraw = CtxDraw(side)
        rulesDraw.forEach { it.apply(ctxDraw) }
        val snap = Snap(pgser.xr, pgser.yr, ctxDraw.dabOnGrids, spots, traces, side == sideTurn, Stage.turn, opterTest)
        return snap
    }

    fun spot(pg: Pg, side: Side): List<Sloy> {
        return rulesSpot.flatMap {
            val ctx = CtxSpot(pg, side,this)
            it.apply(ctx)
            ctx.sloys()
        }
    }

    private fun ensureTest() {
        if (!canEdit) throw Violation("only for test game")
    }

    fun voin(pg: Pg, sideVid: Side): Voin? {
        val ctx = CtxVoin(pg, sideVid)
        rulesVoin.forEach { it.apply(ctx) }
        return ctx.voin
    }

    fun can(from: From, aim: Aim, tp: TpMake): Boolean {
        val ctx = CtxStop(from, aim, tp)
        return !rulesStop.any { it.apply(ctx) }
    }

    fun make(from: From, aim: Aim, tp: TpMake) {
        val ctx = CtxMake(from, aim, tp)
        rulesMake.forEach { it.apply(ctx) }
    }
}

data class From private constructor (val pg:Pg,val sideVoin:Side?,val fromVoin:Boolean){
    /** Источником является юнит со стороной [side]. */
    fun voin(side:Side?):From{
        return copy(sideVoin=side,fromVoin=true)
    }
    companion object {
        fun invoke(pg:Pg) = From(pg,null,false)
    }
}

data class Aim private constructor (val pg:Pg,val sideVoin:Side?,val aimVoin:Boolean){
    /** Целью является только юниты со стороной [side]. */
    fun voin(side:Side?):Aim{
        return copy(sideVoin=side,aimVoin=true)
    }
    companion object {
        fun invoke(pg:Pg) = Aim(pg,null,false)
    }
}

interface Voin {
    val side: Side?
    val life: Int

    fun isEnemy(side: Side) = this.side == side.vs()
    fun isAlly(side: Side) = this.side == side
    fun isNeutral() = this.side == null
}

data class Id(private val code: Int)