package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side

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
                    if (v.hide) drawTile(pg, tileHide)
                    g.make(MsgDraw(this, pg, v))
                }
            }

            spot(10) {
                val voin = voins[pgRaise]
                if (voin != null) {
                    val msg = MsgRaiseVoin(pgRaise, voin)
                    if (g.trap(msg)) {
                        val r = raise(msg.isOn)
                        for (pgNear in pgRaise.near) {
                            r.add(pgNear, tlsMove, EfkMove(pgRaise, pgNear, voin))
                        }
                    }
                }
            }

            stop(0) {
                if (msg is EfkMove && voins[msg.pgTo] != null) msg.stop()
            }

            make(0) {
                when (msg) {
                    is EfkMove -> voins[msg.pgFrom]?.let {
                        if (it === msg.what) {
                            voins.remove(msg.pgFrom)
                            voins[msg.pgTo] = it
                            val xd = msg.pgFrom.x - msg.pgTo.x
                            if (xd != 0) it.flip = xd > 0
                        }
                    }
                    is EfkHide -> voins[msg.pg]?.let {
                        if (it == msg.voin) it.hide = true
                    }
                    is EfkDmg -> voins[msg.pg]?.let {
                        if (it == msg.aim) it.life -= 1
                    }
                }
            }

            trap(0) {
                when (msg) {
                    is EfkHide -> if (!msg.isOk()) voins[msg.pg]?.let {
                        if (it.side == msg.side) msg.voin = it
                    }
                    is EfkDmg -> voins[msg.pg]?.let { msg.aim = it }
                    is EfkEnforce -> voins[msg.pg]?.let { msg.aim = it }
                    is MsgRaiseVoin -> msg.isOn = msg.voin.side == g.sideTurn
                    is MsgRaiseCatapult -> voins[msg.pg]?.let {
                        msg.what = it
                        msg.isOn = it.side == g.sideTurn
                    }
                }
            }

            //            voin(10) {
            //                val v = voins[pg]
            //                if (v != null && v.isVid(side)) put(v)
            //            }

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

class VoinStd(override var side: Side?, var flip: Boolean) : HasLife, HasOwner {
    override var life = 3
    var hide = false
    fun isVid(side: Side) = !hide || isAlly(side)
}

class EfkMove(val pgFrom: Pg, val pgTo: Pg, val what: Any) : Efk() {
    override fun isOk() = true
}

class EfkHide(val pg: Pg, val side: Side, var voin: Any? = null) : Efk() {
    override fun isOk() = voin != null
}

class MsgRaiseVoin(val pg: Pg, val voin: HasOwner) : MsgRaise() {
    override fun isOk() = true
}

class MsgDraw(val ctx: CtxDraw, val pg: Pg, val what: Any) : Msg() {
    override fun isOk() = true
    fun draw(fn: CtxDraw.() -> Unit) {
        ctx.fn()
    }
}

class EfkDmg(val pg: Pg, var aim: Any? = null) : Efk() {
    override fun isOk() = aim != null
}