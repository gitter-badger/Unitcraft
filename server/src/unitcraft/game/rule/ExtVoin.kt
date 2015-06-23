package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class ExtVoin(r: Resource, name: String) {
    val tlsVoin = r.tlsVoin(name)
    val tlsMove = r.tlsAktMove
    val tileHide = r.tile("hide")
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val buildik = r.buildik(tlsVoin.neut)

    fun createRules(r: Rules, land: Land, g: Game): Ruleset {
        val rs = Ruleset(land, g)
        r.rules.addAll(rs.rs)
        return rs
    }

    inner class Ruleset(land: Land, g: Game) {
        val voins = Grid<VoinStd>()

        fun get(pg: Pg) = voins[pg]

        val rs = rules {

            draw(20) {
                for ((pg, v) in voins) {
                    drawTile(pg, tlsVoin(side, v.side), if (v.flip) hintTileFlip else null)
                    drawText(pg, v.life.toString(), hintTextLife)
                    if (g.info(InfoIsHide(v)).hide) drawTile(pg, tileHide)
                    g.make(MsgDrawVoin(this, pg, v))
                }
            }

            spot(10) {
                voins[pgRaise]?.let {
                    val r = raise(MsgRaiseVoin(pgRaise, it))
                    if (r != null) for (pgNear in pgRaise.near) {
                        r.add(pgNear, tlsMove, EfkMove(pgRaise, pgNear, it))
                    }
                }
            }

            stop(0) {
                if (msg is EfkMove && voins[msg.pgTo] != null) msg.stop()
            }

            make(0) {
                when (msg) {
                    is EfkMove -> voins[msg.pgFrom]?.let {
                        if (it === msg.voin) {
                            voins.remove(msg.pgFrom)
                            voins[msg.pgTo] = it
                            val xd = msg.pgFrom.x - msg.pgTo.x
                            if (xd != 0) it.flip = xd > 0
                        }
                    }
                    is EfkDmg -> voins[msg.pg]?.let {
                        if (it == msg.voin) it.life -= 1
                    }
                }
            }

            after(0) {
                when (msg) {
                    is EfkMove -> for (pg in msg.pgTo.near) {
                        g.info(MsgVoin(pg)).voin?.let{
                            g.make(MsgUnhide(pg,it))
                        }
                    }
                }
            }

            info(0) {
                when (msg) {
                    is MsgVoin -> voins[msg.pg]?.let { msg.voins.add(it) }
                    is MsgRaiseVoin -> msg.isOn = msg.voin.side == g.sideTurn
                }
            }

            fun editChange(side: Side, pg: Pg): Boolean {
                val v = voins[pg]
                if (v != null) {
                    when {
                        v.isAlly(side) -> v.side = side.vs()
                        v.isEnemy(side) -> v.side = null
                        v.isNeutral() -> v.side = side
                    }
                    return true
                } else return false
            }

            edit(12, tlsVoin.neut) {
                when (efk) {
                    is EfkEditAdd -> voins[efk.pg] = VoinStd(efk.side, efk.pg.x > efk.pg.pgser.xr / 2)
                    is EfkEditRemove -> consume(voins.remove(efk.pg) != null)
                    is EfkEditChange -> consume(editChange(efk.side, efk.pg))
                }
            }
        }
    }
}

class VoinStd(override var side: Side?, var flip: Boolean) : Voin {
    override var life = 3
}

class EfkMove(val pgFrom: Pg, val pgTo: Pg, val voin: Voin) : Efk()

class EfkHide(val pg: Pg, val side: Side, val voin: Voin) : Efk()

class MsgUnhide(val pg: Pg,val voin:Voin) : Efk()

class MsgRaiseVoin(val pg: Pg, val voin: Voin) : MsgRaise()

class MsgDrawVoin(val ctx: CtxDraw, val pg: Pg, val voin: Voin) : Msg() {
    fun draw(fn: CtxDraw.() -> Unit) {
        ctx.fn()
    }
}

class EfkDmg(val pg: Pg, val voin: Voin) : Efk()

class MsgVoin(val pg:Pg) : Msg(){
    val voins = ArrayList<Voin>()
    val voin : Voin?
        get() = voins.firstOrNull()
}

class InfoIsHide(val voin:Voin,var hide:Boolean = false) : Efk()