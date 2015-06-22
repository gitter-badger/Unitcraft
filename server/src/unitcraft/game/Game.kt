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
//    val rulesVoin: List<RuleVoin>
    val rulesEndTurn: List<RuleEndTurn>
    val rulesTrap: List<RuleTrap>
    val rulesStop: List<RuleStop>
    val rulesMake: List<RuleMake>

    val opterTest: Opter?
    val rulesEdit: List<RuleEdit>?

    init {
        val rules = cdxs.flatMap { it.createRules(land, this) }
        rulesDraw = filterRules<RuleDraw>(rules)
        rulesSpot = filterRules<RuleSpot>(rules)
        rulesTgglRaise = filterRules<RuleTgglRaise>(rules)
//        rulesVoin = filterRules<RuleVoin>(rules)
        rulesEndTurn = filterRules<RuleEndTurn>(rules)
        rulesTrap = filterRules<RuleTrap>(rules)
        rulesMake = filterRules<RuleMake>(rules)
        rulesStop = filterRules<RuleStop>(rules)

        if (canEdit) {
            rulesEdit = filterRules<RuleEdit>(rules)
            opterTest = Opter(rulesEdit.map { Opt(listOf(DabTile(it.tile))) })
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
            'r' -> editRemove(prm)
            'd' -> editDestroy(prm)
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
        val ctx = CtxEdit(EfkEditAdd(prm.pg(0), side))
        rulesEdit!!.elementAtOrElse(prm.int(2)) { throw Violation("editAdd out bound") }.apply(ctx)
    }

    private fun editRemove(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        edit(EfkEditRemove(prm.pg(0)),true)
    }

    private fun editDestroy(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        edit(EfkEditDestroy(prm.pg(0)))
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        edit(EfkEditChange(prm.pg(0), side))
    }

    private fun edit(efk:EfkEdit,isReverse:Boolean = false){
        for (it in if(isReverse) rulesEdit!!.reverse() else rulesEdit!! ) {
            val ctx = CtxEdit(efk)
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
        make(akt.efk)
        println("akt "+side+" from "+prm.pg(0) + " index "+prm.int(2)+" to "+prm.pg(3))
    }

    private fun aktOpt(side: Side, prm: Prm) {
        prm.ensureSize(6)
        val sloy = spot(prm.pg(0),side)[prm.int(2)]
        if(!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3))?:throw Violation("akt not found")
        traces.clear()
        //make(akt.efkOpt) prm.int(5)
        println("akt "+side+" from "+prm.pg(0) + " index "+prm.int(2)+" to "+prm.pg(3)+" opt "+prm.int(5))
    }

    private fun endTurn(side: Side, prm: Prm) {
        prm.ensureSize(0)
        if (sideTurn != side) throw Violation("endTurn side($side) != sideTurn($sideTurn)")
        rulesEndTurn.forEach { it.apply() }
        sideTurn = sideTurn.vs()
    }

    private fun snap(side: Side): Snap {
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

    private fun spot(pg: Pg, side: Side): List<Sloy> {
        return rulesSpot.flatMap {
            val ctx = CtxSpot(this,pg, side)
            it.apply(ctx)
            ctx.sloys()
        }
    }

    private fun ensureTest() {
        if (!canEdit) throw Violation("only for test game")
    }

//    fun voin(pg: Pg, sideVid: Side): Voin? {
//        val ctx = CtxVoin(pg, sideVid)
//        rulesVoin.forEach { it.apply(ctx) }
//        return ctx.voin
//    }

    fun trap(msg:Msg): Boolean {
        val ctx = CtxTrap(msg)
        rulesTrap.forEach { it.apply(ctx) }
        return msg.isOk() && !stop(msg)
    }

    private fun stop(msg:Msg):Boolean{
        val ctx = CtxStop(msg)
        rulesStop.forEach { it.apply(ctx) }
        return msg.isStoped && !refute(msg)
    }

    private fun refute(msg:Msg):Boolean{
        return false
    }

    fun make(msg:Msg) {
        val ctx = CtxMake(msg)
        rulesMake.forEach { it.apply(ctx) }
    }
}

interface HasOwner{
    val side: Side?
    fun isEnemy(side: Side) = this.side == side.vs()
    fun isAlly(side: Side) = this.side == side
    fun isNeutral() = this.side == null
}

interface HasLife{
    val life: Int
}
