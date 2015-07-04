package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import java.util.*

abstract class CdxVoin(r: Resource) : Cdx(r) {
    val resVoin = r.resVoin
}

class RulesVoin(fn:RulesVoin.()->Unit) : Rules(){
    init{
        this.fn()
    }

    fun ruleVoin(g: Game,voins: Grid<VoinStd>,resVoin: ResVoin,tlsVoin: TlsVoin){
        ruleEditAdd(voins,tlsVoin)
        ruleEditChangeAndRemove(voins)
        ruleSolid(voins)
        ruleDrawUnit(voins,g,tlsVoin,resVoin)
        ruleEfk(voins,g)
    }

    fun ruleEditAdd(voins: Grid<VoinStd>,tlsVoin: TlsVoin){
        editAdd(12, tlsVoin.neut) {
            voins[pgEdit] = VoinStd(sideVid, 5, pgEdit.x > pgEdit.pgser.xr / 2)
        }
    }

    fun ruleEditChangeAndRemove(voins: Grid<out VoinStd>){
        fun editChange(side: Side, pg: Pg): Boolean {
            val v = voins[pg]
            if (v != null) {
                when {
                    v.isAlly(side) -> v.side = side.vs
                    v.isEnemy(side) -> v.side = null
                    v.side == null -> v.side = side
                }
                return true
            } else return false
        }

        make<EfkEditChange>(12) {
            if (editChange(sideVid, pgEdit)) eat()
        }

        make<EfkEditRemove>(-12) {
            if (voins.remove(pgEdit)) eat()
        }
    }


    fun ruleSolid(voins: Grid<out VoinStd>){
        stop<EfkMove>(0) {
            if (voins[pgTo] != null) stop()
        }

        stop<EfkBuild>(0) {
            if (voins[pg] != null) stop()
        }
    }

    fun ruleDrawUnit(voins: Grid<out VoinStd>,g: Game,tlsVoin: TlsVoin,resVoin: ResVoin){
        info<MsgDraw>(20) {
            for ((pg, v) in voins) {
                drawTile(pg, tlsVoin(sideVid, v.side), if (v.flip) resVoin.hintTileFlip else null)
                drawText(pg, v.life.toString(), resVoin.hintTextLife)
                if (g.info(MsgIsHided(v)).isHided) drawTile(pg, resVoin.tileHide)
                g.info(MsgDrawVoin(this, pg, v))
            }
        }
    }

    fun <T:VoinStd> ruleEfk(voins: Grid<T>,g: Game){
        make<EfkRemove>(0) {
            if (voins[pg] == obj) {
                voins.remove(pg)
                eat()
            }
        }

        make<EfkHeal>(0) {
            voins[pg]?.let {
                if (it == voin) it.life += value
            }
        }

        make<EfkDmg>(0) {
            voins[pgAim]?.let {
                if (it == voin) it.life -= dmg
            }
        }

        make<EfkMove>(0) {
            voins[pgFrom]?.let {
                if (it === voin) {
//                    voins[pgTo] = voins.remove(pgFrom)!!
                    val xd = pgFrom.x - pgTo.x
                    if (xd != 0) it.flip = xd > 0
                }
            }
        }

        /** обычный юнит видит скрытых рядом с собой */
        after<EfkMove>(0) {
            voins[pgTo]?.let {
                g.make(EfkUnhide(pgTo.near, it))
            }
        }

        info<MsgVoin>(0) {
            voins[pg]?.let { all.add(it) }
        }
    }

    fun ruleTgglRaiseBySideTurn(g: Game,voins: Grid<VoinStd>){
        info<MsgTgglRaise>(0) {
//            if (voins[pgRaise] == voinRaise) isOn = voinRaise.side == g.sideTurn
        }
    }

    fun aimByHand(g: Game,voins: Grid<VoinStd>,resVoin: ResVoin,aim:(Pg, Pg, VoinStd,Side, Raise)->Unit) {
        ruleTgglRaiseBySideTurn(g,voins)

        info<MsgSpot>(0) {
            if(voins[pgSrc]==null) return@info
            voins[pgSpot]?.let { voinSpot ->
                if (!g.info(MsgIsHided(voinSpot)).isHided) {
                    val tggl = g.info(MsgTgglRaise(pgSpot, voinSpot))
//                    if (g.sideTurn != side) tggl.isOn = false
                    if (!tggl.isCanceled) {
                        val r = Raise(pgSpot, tggl.isOn)
                        for (pgNear in pgSpot.near) aim(pgNear, pgSpot, voinSpot, side, r)
                        for (pgNear in pgSpot.near) {
                            val efk = EfkMove(pgSpot, pgNear, voinSpot)
                            if (!g.stop(efk)) r.add(pgNear, resVoin.tlsMove, efk)
                        }
                        add(r)
                    }
                }
            }
        }
    }
}

open class VoinStd(override var side: Side?, override var life: Int, var flip: Boolean) : Voin{
    var isTired = true
        private set

    var enforced = true

    fun tire(){
        isTired = true
    }

    fun isReady(sideTurn:Side) =  sideTurn == side ||  enforced

}

class EfkMove(val pgFrom: Pg, val pgTo: Pg, val voin: Voin) : Efk()

class EfkHide(val pg: Pg, val side: Side, val voin: Voin) : Efk()

class EfkUnhide(val pg: List<Pg>, val voin: Voin) : Efk()

class MsgDrawVoin(val ctx: MsgDraw, val pg: Pg, val voin: Voin) : Msg() {
    fun drawTile(pg: Pg, tile: Int, hint: Int? = null) {
        ctx.drawTile(pg, tile, hint)
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        ctx.drawText(pg, text, hint)
    }
}

class EfkDmg(val pgAim: Pg, val voin: Voin, val dmg: Int = 1) : Efk()

class MsgVoin(val pg: Pg) : Msg() {
    val all = ArrayList<Voin>(1)

    fun add(voin: Voin) {
        all.add(voin)
    }

    val voin: Voin?
        get() = all.firstOrNull()
}

class MsgIsHided(val voin: Voin) : Msg() {
    var isHided: Boolean = false
        private set

    fun yes() {
        isHided = true
    }

    fun isVid(side: Side) = !isHided || side == voin.side
}

class EfkRemove(val pg: Pg, val obj: Any) : Efk()

class EfkHeal(val pg: Pg, val voin: Voin, val value: Int = 1) : Efk()

class EfkBuild(val pg: Pg) : Efk()