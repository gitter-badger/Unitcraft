package unitcraft.game

import unitcraft.land.Land
import unitcraft.server.*
import java.util.ArrayList
import java.util.HashMap

class Game(cdxs: List<Cdx>, land: Land, val canEdit: Boolean = false) : IGame {
    val pgser = land.pgser
    val pgs = pgser.pgs
    var sideTurn = Side.a
        private set
    val bonus = HashMap<Side, Int>()

    val rulesEndTurn: List<RuleEndTurn>
    val rulesInfo: List<RuleInfo>
    val rulesStop: List<RuleStop>
    val rulesMake: List<RuleMake>
    val rulesAfter: List<RuleAfter>

    val opterTest: Opter?
    val rulesEdit: List<RuleEdit>?

    init {
        val rules = cdxs.flatMap { it.createRules(land, this) }
        rulesEndTurn = filterRules<RuleEndTurn>(rules)
        rulesInfo = filterRules<RuleInfo>(rules)
        rulesMake = filterRules<RuleMake>(rules)
        rulesStop = filterRules<RuleStop>(rules)
        rulesAfter = filterRules<RuleAfter>(rules)

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
        edit(EfkEditRemove(prm.pg(0)), true)
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

    private fun edit(efk: EfkEdit, isReverse: Boolean = false) {
        for (it in if (isReverse) rulesEdit!!.reverse() else rulesEdit!! ) {
            val ctx = CtxEdit(efk)
            it.apply(ctx)
            if (efk.isEated) return
        }
    }

    private fun akt(side: Side, prm: Prm) {
        prm.ensureSize(5)
        val sloy = spot(prm.pg(0), side)[prm.int(2)]
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3)) ?: throw Violation("akt not found")
        traces.clear()
        make(akt.efk)
        println("akt " + side + " from " + prm.pg(0) + " index " + prm.int(2) + " to " + prm.pg(3))
    }

    private fun aktOpt(side: Side, prm: Prm) {
        prm.ensureSize(6)
        val sloy = spot(prm.pg(0), side)[prm.int(2)]
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3)) ?: throw Violation("akt not found")
        traces.clear()
        //make(akt.efkOpt) prm.int(5)
        println("akt " + side + " from " + prm.pg(0) + " index " + prm.int(2) + " to " + prm.pg(3) + " opt " + prm.int(5))
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
        val snap = Snap(pgser.xr, pgser.yr, info(MsgDraw(side)).dabOnGrids, spots, traces, side == sideTurn, Stage.turn, opterTest)
        return snap
    }

    private fun spot(pg: Pg, side: Side): List<Sloy> {
        return info(MsgSpot(pg, side)).sloys()
    }

    private fun ensureTest() {
        if (!canEdit) throw Violation("only for test game")
    }

    //    fun voin(pg: Pg, sideVid: Side): Voin? {
    //        val ctx = CtxVoin(pg, sideVid)
    //        rulesVoin.forEach { it.apply(ctx) }
    //        return ctx.voin
    //    }
    /**
     * Напололняет [msg] разными данными, передается всем подписавшимся правилам в порядке приоритета. Нельзя менять состояние правил.
     */
    fun <T : Msg> info(msg: T): T {
        val ctx = CtxInfo(msg)
        rulesInfo.forEach { it.apply(ctx) }
        return msg
    }

    /**
     * Выполняет [efk], передается всем подписавшимся правилам в порядке приоритета, пока одно из них не съест [efk].
     * Можно менять состояние правил. После вызывается событие after.
     */
    fun make(efk: Efk) {
        val ctx = CtxMake(efk)
        if (!stop(efk)) for (it in rulesMake) {
            it.apply(ctx)
            if(efk.isEated) {
                rulesAfter.forEach { it.apply(ctx) }
                return
            }
        }
    }

    /**
     * Проверка на выполнимость [efk], передается всем правилам в порядке приоритета, пока одно из них не запретит [efk].
     * После запрета проверяется допустимость запрета.
     */
    fun stop(efk: Efk): Boolean {
        val ctx = CtxMake(efk)
        for (it in rulesStop) {
            it.apply(ctx)
            if(efk.isStoped && !refute(efk)) return true
        }
        return false
    }

    private fun refute(efk: Efk): Boolean {
        return false
    }
}

interface Voin : Obj {
    val life: Int
    val side: Side?
    fun isEnemy(side: Side) = this.side == side.vs()
    fun isAlly(side: Side) = this.side == side
}

class MsgSpot(val pg: Pg, val side: Side) : Msg() {
    val raises = ArrayList<MsgRaise>()

    fun add(raise: MsgRaise) {
        //        if (g.sideTurn == side) raise.isOn = false
        raises.add(raise)
    }

    fun sloys(): List<Sloy> {
        // схлопнуть, если нет пересечений
        return raises.flatMap { it.sloys() }
    }
}

class MsgRaise(private val g: Game, val pg: Pg, val src: Obj, val voinEfk: Voin) : Msg() {
    private val listSloy = ArrayList<Sloy>()
    var isOn = false

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, efk: Efk) {
        if (!g.stop(efk)) addAkt(Akt(pgAkt, tlsAkt(isOn), efk, null))
    }
    //    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        if (akt.pgAim == pg) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // заполнить пустоты сверху снизу
        return listSloy
    }
}

interface Obj

abstract class Flat : Obj