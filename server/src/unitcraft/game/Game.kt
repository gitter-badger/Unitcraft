package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.*
import java.util.ArrayList
import java.util.HashMap

class Game(val pgser: Pgser, canEdit: Boolean = false, stazis: Stazis, place: Place,val herds: List<Herd>,val drawer:Drawer) : IGame {

    val pgs = pgser.pgs
    var sideTurn = Side.a
        private set
    val bonus = HashMap<Side, Int>()

    val opterTest: Opter?

    init {
        opterTest = if (!canEdit) {
            null
        } else {
            drawer.opterTest()
        }
    }

    val traces = Traces()

    override fun cmd(side: Side, cmd: String) {
        if(side.isN) throw throw Err("side is neutral")
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
        val num = prm.int(2)
        if(num >= opterTest!!.opts.size()) throw Violation("editAdd out bound")
        drawer.editAdd(prm.pg(0),side,num)
    }

    private fun editRemove(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        drawer.editRemove(prm.pg(0))
    }

    private fun editDestroy(prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        drawer.editDestroy(prm.pg(0))
    }

    private fun editChange(side: Side, prm: Prm) {
        ensureTest()
        prm.ensureSize(2)
        drawer.editChange(prm.pg(0), side)
    }

    private fun akt(side: Side, prm: Prm) {
        prm.ensureSize(5)
        val sloy = spot(prm.pg(0), side)[prm.int(2)]
        if (!sloy.isOn) throw Violation("sloy is off")
        val akt = sloy.aktByPg(prm.pg(3)) ?: throw Violation("akt not found")
        traces.clear()
        akt.efk?.let { make(it) }
        akt.fn?.invoke()
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
        sideTurn = sideTurn.vs
    }

    private fun snap(side: Side): Snap {
        val spots = HashMap<Pg, List<Sloy>>()
        for (pg in pgs) {
            val sloys = spot(pg, side)
            if (sloys.isNotEmpty()) spots[pg] = sloys
        }
        val snap = Snap(pgser.xr, pgser.yr, drawer.draw(side), spots, traces, side == sideTurn, Stage.turn, opterTest)
        return snap
    }

    private fun spot(pg: Pg, side: Side): List<Sloy> {
        return info(MsgSpot(this, pg, side)).sloys()
    }

    private fun ensureTest() {
        if (opterTest == null) throw Violation("only for test game")
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
        //        rulesInfo[msg.javaClass.kotlin]?.forEach {
        //            if(!msg.isEated) it.apply(msg) else return msg
        //        }
        return msg
    }

    /**
     * Выполняет [efk], передается всем подписавшимся правилам в порядке приоритета, пока одно из них не съест [efk].
     * Можно менять состояние правил. После вызывается событие after.
     */
    fun make(efk: Efk) {
        //        if (!stop(efk)) rulesMake[efk.javaClass.kotlin]?.forEach {
        //            it.apply(efk)
        //            if(efk.isEated) {
        //                after(efk)
        //                return
        //            }
        //        }
    }

    fun after(efk: Efk) {
        //        rulesAfter[efk.javaClass.kotlin]?.forEach { it.apply(efk) }
    }

    /**
     * Проверка на выполнимость [efk], передается всем правилам в порядке приоритета, пока одно из них не запретит [efk].
     * После запрета проверяется допустимость запрета.
     */
    fun stop(efk: Efk): Boolean {
        //        rulesStop[efk.javaClass.kotlin]?.forEach { it ->
        //            it.apply(efk)
        //            if(efk.isStoped && !refute(efk)) return true
        //        }
        return false
    }

    private fun refute(efk: Efk): Boolean {
        return false
    }

    companion object {
        private fun <K> sortRules(map: Map<K, List<Rule>>) = map.mapValues { it.value.sortBy { it.prior } }
    }

    fun voin(pg: Pg, side: Side) = info(MsgVoin(pg)).all.firstOrNull() {
        info(MsgIsHided(it)).isVid(side)
    }

    fun voins(pg: Pg, side: Side) = info(MsgVoin(pg)).all.filter {
        info(MsgIsHided(it)).isVid(side)
    }
}

interface Voin {
    val life: Int
    val side: Side?
    fun isEnemy(side: Side) = this.side == side.vs
    fun isAlly(side: Side) = this.side == side
}

class MsgSpot(private val g: Game, val pgSpot: Pg, val side: Side, val pgSrc: Pg = pgSpot) : Msg() {
    val raises = ArrayList<Raise>()

    fun raise(pgRaise: Pg, voinRaise: Voin, src: Any) {
        //val tggl = g.info(MsgTgglRaise(pgRaise,src,voinRaise))
        //if (g.sideTurn != side) tggl.isOn = false
        //if(!tggl.isCanceled) raises.add(g.info(MsgRaise(g,pgRaise,src,voinRaise,tggl.isOn,side)))
    }

    fun add(r: Raise) {
        raises.add(r)
    }

    fun sloys(): List<Sloy> {
        // схлопнуть, если нет пересечений
        return raises.flatMap { it.sloys() }
    }
}

class MsgTgglRaise(val pgRaise: Pg, val voinRaise: Voin) : Msg() {
    var isOn = false
    var isCanceled = false
        private set

    fun cancel() {
        isCanceled = true
    }
}

class Raise(val pgRaise: Pg, val isOn: Boolean) : Msg() {
    private val listSloy = ArrayList<Sloy>()

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, efk: Efk) {
        addAkt(Akt(pgAkt, tlsAkt(isOn), efk, null))
    }

    fun addFn(pgAkt: Pg, tlsAkt: TlsAkt, fn: () -> Unit) {
        addAkt(Akt(pgAkt, tlsAkt(isOn), null, null, fn))
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

//class MsgRaise(private val g: Game, val pgRaise: Pg, val src: Obj, val voinRaise: Voin, val isOn:Boolean,val sideVid:Side) : Msg() {
//    private val listSloy = ArrayList<Sloy>()
//
//    fun add(pgAkt: Pg, tlsAkt: TlsAkt, efk: Efk) {
//        if (!g.stop(efk)) addAkt(Akt(pgAkt, tlsAkt(isOn), efk, null))
//    }
//    //    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))
//
//    private fun addAkt(akt: Akt) {
//        if (akt.pgAim == pgRaise) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
//        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
//        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
//        listSloy[idx].akts.add(akt)
//    }
//
//    fun sloys(): List<Sloy> {
//        // заполнить пустоты сверху снизу
//        return listSloy
//    }
//}

object EfkEndTurn : Efk()

interface OnDraw {
    fun draw(ctx: MsgDraw)
    fun editAdd(): List<Pair<Int, (Pg, Side) -> Unit>> = emptyList()
    fun editRemove(pg: Pg) = false
}