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
        r.addRules(rs.rs)
        return rs
    }

    inner class Ruleset(land: Land, g: Game) {
        val voins = Grid<VoinStd>()

        fun get(pg: Pg) = voins[pg]

        val rs = rules {

            info<MsgDraw>(20) {
                for ((pg, v) in voins) {
                    drawTile(pg, tlsVoin(sideVid, v.side), if (v.flip) hintTileFlip else null)
                    drawText(pg, v.life.toString(), hintTextLife)
                    if (g.info(InfoIsHide(v)).isHided) drawTile(pg, tileHide)
                    g.info(MsgDrawVoin(this, pg, v))
                }
            }

            info<MsgSpot>(0) {
                voins[pgSpot]?.let {
                    add(g.info(MsgRaise(g, pgSpot, it, it)))
                }
            }

            info<MsgRaise>(10) {
                if (voins.containsValue(src))
                    for (pgNear in pgRaise.near) {
                        add(pgNear, tlsMove, EfkMove(pgRaise, pgNear, voinEfk))
                    }
            }

            stop<EfkMove>(0) {
                if (voins[pgTo] != null) stop()
            }

            stop<EfkBuild>(0) {
                if (voins[pg] != null) stop()
            }

            make<EfkRemove>(0) {
               voins.values().remove(obj)
            }

            make<EfkHeal>(0){
                voins[pg]?.let {
                    if (it == voin) it.life += value
                }
            }

            make<EfkDmg>(0){
                voins[pgAim]?.let {
                    if (it == voin) it.life -= value
                }
            }
            make<EfkMove>(0) {
                voins[pgFrom]?.let {
                    if (it === voin) {
                        voins.remove(pgFrom)
                        voins[pgTo] = it
                        val xd = pgFrom.x - pgTo.x
                        if (xd != 0) it.flip = xd > 0
                    }
                }
            }

            after<EfkMove>(0) {
                for (pg in pgTo.near) {
                    g.info(MsgVoin(pg)).voin?.let {
                        g.make(EfkUnhide(pg, it))
                    }
                }
            }

            info<MsgVoin>(0) {
                voins[pg]?.let { all.add(it) }
            }

            info<MsgRaise>(0) {
                isOn = voinEfk.side == g.sideTurn
            }

            fun editChange(side: Side, pg: Pg): Boolean {
                val v = voins[pg]
                if (v != null) {
                    when {
                        v.isAlly(side) -> v.side = side.vs()
                        v.isEnemy(side) -> v.side = null
                        v.side == null -> v.side = side
                    }
                    return true
                } else return false
            }

            editAdd(12, tlsVoin.neut) {
                voins[pgEdit] = VoinStd(sideVid, pgEdit.x > pgEdit.pgser.xr / 2)
            }

            make<EfkEditChange>(12) {
                if (editChange(sideVid, pgEdit)) eat()
            }

            make<EfkEditRemove>(12) {
                if (voins.remove(pgEdit) != null) eat()
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

class EfkDmg(val pgAim: Pg, val voin: Voin, val value: Int = 1) : Efk()

class MsgVoin(val pg: Pg) : Msg() {
    val all = ArrayList<Voin>(1)

    fun add(voin: Voin) {
        all.add(voin)
    }

    val voin: Voin?
        get() = all.firstOrNull()
}

class InfoIsHide(val voin: Voin) : Msg() {
    var isHided: Boolean = false
        private set

    fun hide() {
        isHided = true
    }
}

class EfkRemove(val pg: Pg, val obj: Obj) : Efk()

class EfkHeal(val pg: Pg, val voin: Voin, val value: Int = 1) : Efk()

class EfkBuild(val pg: Pg) : Efk()