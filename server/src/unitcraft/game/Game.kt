package unitcraft.game

import unitcraft.land.Land
import unitcraft.server.*
import java.util.ArrayList
import java.util.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlin

class Game(cdxs: List<Cdx>, land: Land, val canEdit: Boolean = false) : IGame {
    val pgser = land.pgser
    val pgs = pgser.pgs
    var sideTurn = Side.a
        private set
    val bonus = HashMap<Side, Int>()

    val rulesInfo: Map<KClass<out Msg>,List<Rule>>
    val rulesStop: Map<KClass<out Efk>,List<Rule>>
    val rulesMake: Map<KClass<out Efk>,List<Rule>>
    val rulesAfter: Map<KClass<out Efk>,List<Rule>>

    val opterTest: Opter?
//    val rulesEdit: List<RuleEdit>?

    init {
        val rules = cdxs.map { it.createRules(land,this) }.reduce { rules, r -> rules.addRules(r) }
        rulesInfo = sortRules(rules.rulesInfo)
        rulesStop = sortRules(rules.rulesStop)
        rulesMake = sortRules(rules.rulesMake)
        rulesAfter = sortRules(rules.rulesAfter)
        opterTest = if (canEdit) Opter(rules.tilesEditAdd.sortBy{it.first}.map { Opt(listOf(DabTile(it.second))) }) else null

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
        val efk = EfkEditAdd(prm.pg(0), side)
        rulesMake[efk.javaClass.kotlin]?.elementAtOrElse(prm.int(2)) { throw Violation("editAdd out bound") }?.apply?.invoke(efk)
    }

    private fun editRemove(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        make(EfkEditRemove(prm.pg(0)))
    }

    private fun editDestroy(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        make(EfkEditDestroy(prm.pg(0)))
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        make(EfkEditChange(prm.pg(0), side))
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
        after(EfkEndTurn)
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
        rulesInfo[msg.javaClass.kotlin]?.forEach { it.apply(msg) }
        return msg
    }

    /**
     * Выполняет [efk], передается всем подписавшимся правилам в порядке приоритета, пока одно из них не съест [efk].
     * Можно менять состояние правил. После вызывается событие after.
     */
    fun make(efk: Efk) {
        if (!stop(efk)) rulesMake[efk.javaClass.kotlin]?.forEach {
            it.apply(efk)
            if(efk.isEated) {
                after(efk)
                return
            }
        }
    }

    fun after(efk:Efk){
        rulesAfter[efk.javaClass.kotlin]?.forEach { it.apply(efk) }
    }

    /**
     * Проверка на выполнимость [efk], передается всем правилам в порядке приоритета, пока одно из них не запретит [efk].
     * После запрета проверяется допустимость запрета.
     */
    fun stop(efk: Efk): Boolean {
        rulesStop[efk.javaClass.kotlin]?.forEach { it ->
            it.apply(efk)
            if(efk.isStoped && !refute(efk)) return true
        }
        return false
    }

    private fun refute(efk: Efk): Boolean {
        return false
    }

    companion object {
        private fun <K> sortRules(map: Map<K, List<Rule>>) = map.mapValues { it.value.sortBy { it.prior } }
    }
}

interface Voin : Obj {
    val life: Int
    val side: Side?
    fun isEnemy(side: Side) = this.side == side.vs()
    fun isAlly(side: Side) = this.side == side
}

class MsgSpot(val pgSpot: Pg, val side: Side) : Msg() {
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

class MsgRaise(private val g: Game, val pgRaise: Pg, val src: Obj, val voinRaise: Voin) : Msg() {
    private val listSloy = ArrayList<Sloy>()
    var isOn = false
    var isStoped = false

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, efk: Efk) {
        if (!isStoped && !g.stop(efk)) addAkt(Akt(pgAkt, tlsAkt(isOn), efk, null))
    }
    //    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        if (akt.pgAim == pgRaise) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
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

object EfkEndTurn : Efk()