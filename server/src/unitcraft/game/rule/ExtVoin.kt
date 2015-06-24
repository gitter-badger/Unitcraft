package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.ArrayList

class ExtVoin(r: Resource, name: String) {
    val tlsVoin = r.tlsVoin(name)
    val tlsMove = r.tlsAktMove
    val tileHide = r.tileHide
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

            info(20) {
                if(msg is MsgDraw)
                for ((pg, v) in voins) {
                    msg.drawTile(pg, tlsVoin(msg.side, v.side), if (v.flip) hintTileFlip else null)
                    msg.drawText(pg, v.life.toString(), hintTextLife)
                    if (g.info(InfoIsHide(v)).hide) msg.drawTile(pg, tileHide)
                    g.info(MsgDrawVoin(msg, pg, v))
                }
            }

            info(0) {
                if (msg is MsgSpot) {
                    voins[msg.pg]?.let{
                        msg.add(g.info(MsgRaise(g, msg.pg,it,it)))
                    }
                }
            }

            info(10) {
                if (msg is MsgRaise) if (voins.containsValue(msg.src))
                    for (pgNear in msg.pg.near) {
                        msg.add(pgNear, tlsMove, EfkMove(msg.pg, pgNear, msg.voinEfk))
                    }
            }

            stop(0) {
                if (efk is EfkMove) if (voins[efk.pgTo] != null) efk.stop()
                if (efk is EfkBuild) if(voins[efk.pg]!=null) efk.stop()
            }

            make(0) {
                when (efk) {
                    is EfkMove -> voins[efk.pgFrom]?.let {
                        if (it === efk.voin) {
                            voins.remove(efk.pgFrom)
                            voins[efk.pgTo] = it
                            val xd = efk.pgFrom.x - efk.pgTo.x
                            if (xd != 0) it.flip = xd > 0
                        }
                    }
                    is EfkDmg -> voins[efk.pg]?.let {
                        if (it == efk.voin) it.life -= efk.value
                    }
                    is EfkHeal -> voins[efk.pg]?.let {
                        if (it == efk.voin) it.life += efk.value
                    }
                    is EfkRemove -> voins.values().remove(efk.obj)
                }
            }

            after(0) {
                when (efk) {
                    is EfkMove -> for (pg in efk.pgTo.near) {
                        g.info(MsgVoin(pg)).voin?.let {
                            g.make(EfkUnhide(pg, it))
                        }
                    }
                }
            }

            info(0) {
                when (msg) {
                    is MsgVoin -> voins[msg.pg]?.let { msg.voins.add(it) }
                    is MsgRaise -> msg.isOn = msg.voinEfk.side == g.sideTurn
                }
            }

            fun editChange(side: Side, pg: Pg): Boolean {
                val v = voins[pg]
                if (v != null) {
                    when {
                        v.isAlly(side) -> v.side = side.vs()
                        v.isEnemy(side) -> v.side = null
                        v.side==null -> v.side = side
                    }
                    return true
                } else return false
            }

            edit(12, tlsVoin.neut) {
                when (efk) {
                    is EfkEditAdd -> voins[efk.pg] = VoinStd(efk.side, efk.pg.x > efk.pg.pgser.xr / 2)
                    is EfkEditRemove -> if(voins.remove(efk.pg) != null) efk.eat()
                    is EfkEditChange -> if(editChange(efk.side, efk.pg)) efk.eat()
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

class EfkUnhide(val pg: Pg, val voin: Voin) : Efk()

class MsgDrawVoin(val ctx: MsgDraw, val pg: Pg, val voin: Voin) : Msg() {
    fun drawTile(pg: Pg, tile: Int, hint: Int? = null) {
        ctx.drawTile(pg, tile, hint)
    }

    fun drawText(pg: Pg, text: String, hint: Int? = null) {
        ctx.drawText(pg, text, hint)
    }
}

class EfkDmg(val pg: Pg, val voin: Voin,val value:Int = 1) : Efk()

class MsgVoin(val pg: Pg) : Msg() {
    val voins = ArrayList<Voin>(1)
    val voin: Voin?
        get() = voins.firstOrNull()
}

class InfoIsHide(val voin: Voin, var hide: Boolean = false) : Msg()

class EfkRemove(val pg:Pg,val obj:Obj) : Efk()

class EfkHeal(val pg:Pg,val voin:Voin,val value:Int = 1) : Efk()

class EfkBuild(val pg:Pg) : Efk()