package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import java.util.ArrayList

class ExtVoin(r: Resource, name: String) {
    val tlsVoin = r.tlsVoin(name)
    val tlsMove = r.tlsAktMove
    val tileHide = r.tileHide
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val buildik = r.buildik(tlsVoin.neut)

    fun createRules(r: Rules, g: Game, voins: Voins) {
        r.addRules(initRules(g, voins))
    }

    interface Voins : Sequence<Pair<Pg, Voins.VoinMut>> {
        //        fun sequence():Sequence<Pair<Pg,VoinMut>>
        fun get(pg: Pg): VoinMut?

        fun move(pgFrom: Pg, pgTo: Pg)
        fun create(pg: Pg, side: Side, life: Int, flip: Boolean)
        fun remove(pg: Pg): Boolean

        interface VoinMut : Voin {
            override var side: Side?
            override var life: Int
            var flip: Boolean
        }

        fun has(obj: Obj): Boolean
    }

    open class VoinStd(override var side: Side?, override var life: Int, override var flip: Boolean) : Voins.VoinMut

    private fun initRules(g: Game, voins: Voins) = rules {

        info<MsgDraw>(20) {
            for ((pg, v) in voins) {
                drawTile(pg, tlsVoin(sideVid, v.side), if (v.flip) hintTileFlip else null)
                drawText(pg, v.life.toString(), hintTextLife)
                if (g.info(MsgIsHided(v)).isHided) drawTile(pg, tileHide)
                g.info(MsgDrawVoin(this, pg, v))
            }
        }

        info<MsgSpot>(0) {
            voins[pgSpot]?.let {
                if (!g.info(MsgIsHided(it)).isHided) raise(pgSpot, it, it)
            }
        }

        info<MsgRaise>(10) {
            if (voins.has(src)) for (pgNear in pgRaise.near) {
                add(pgNear, tlsMove, EfkMove(pgRaise, pgNear, voinRaise))
            }
        }

        info<MsgTgglRaise>(0) {
            if (voins[pgRaise] == voinRaise) isOn = voinRaise.side == g.sideTurn
        }

        stop<EfkMove>(0) {
            if (voins[pgTo] != null) stop()
        }

        stop<EfkBuild>(0) {
            if (voins[pg] != null) stop()
        }

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
                    voins.move(pgFrom, pgTo)
                    val xd = pgFrom.x - pgTo.x
                    if (xd != 0) it.flip = xd > 0
                }
            }
        }

        after<EfkMove>(0) {
            voins[pgTo]?.let {
                g.make(EfkUnhide(pgTo.near, it))
            }
        }

        info<MsgVoin>(0) {
            voins[pg]?.let { all.add(it) }
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
            voins.create(pgEdit, sideVid, 3, pgEdit.x > pgEdit.pgser.xr / 2)
        }

        make<EfkEditChange>(12) {
            if (editChange(sideVid, pgEdit)) eat()
        }

        make<EfkEditRemove>(-12) {
            if (voins.remove(pgEdit)) eat()
        }

    }

    companion object {
        fun <T : Voins.VoinMut> fromGrid(grid: Grid<T>, create: (Side, Int, Boolean) -> T) = object : Voins {
            override fun iterator() = grid.asSequence().map { it.toPair() }.iterator()

            override fun has(obj: Obj) = grid.containsValue(obj)

            override fun get(pg: Pg) = grid[pg]

            override fun move(pgFrom: Pg, pgTo: Pg) {
                grid[pgTo] = grid.remove(pgFrom)!!
            }

            override fun create(pg: Pg, side: Side, life: Int, flip: Boolean) {
                grid[pg] = create(side, life, flip)
            }

            override fun remove(pg: Pg) = grid.remove(pg) != null
        }

        fun std() = fromGrid(Grid<VoinStd>(), ::VoinStd)
    }
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

class EfkRemove(val pg: Pg, val obj: Obj) : Efk()

class EfkHeal(val pg: Pg, val voin: Voin, val value: Int = 1) : Efk()

class EfkBuild(val pg: Pg) : Efk()