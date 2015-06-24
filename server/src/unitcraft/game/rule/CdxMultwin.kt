package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class CdxMultwin(r: Resource) : Cdx(r) {
    val name = "multwin"
    //val tlsAkt = r.tlsAkt(name)

    val tlsVoin = r.tlsVoin(name)
    val tlsMove = r.tlsAktMove
    val tileHide = r.tileHide
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val buildik = r.buildik(tlsVoin.neut)

    override fun createRules(land: Land, g: Game) = rules {
        val voins = Grid<Multwin>()

        val coreBySide = HashMap<Side,MultwinCore>()

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
                val r = raise(MsgRaise(pgRaise, it))
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
                    if (it == msg.voin) it.core.life -= 1
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
                is MsgRaise -> msg.isOn = msg.voin.side == g.sideTurn
            }
        }

        fun editChange(side: Side, pg: Pg): Boolean {
            val v = voins[pg]
            if (v != null) {
                when {
                    v.isAlly(side) -> v.core.side = side.vs()
                    v.isEnemy(side) -> v.core.side = null
                    v.isNeutral() -> v.core.side = side
                }
                return true
            } else return false
        }

        edit(12, tlsVoin.neut) {
            when (efk) {
                is EfkEditAdd ->{
                    val core = coreBySide.getOrPut(efk.side){MultwinCore(efk.side)}
                    voins[efk.pg] = Multwin(core, efk.pg.x > efk.pg.pgser.xr / 2)
                }
                is EfkEditRemove -> consume(voins.remove(efk.pg) != null)
                is EfkEditChange -> consume(editChange(efk.side, efk.pg))
            }
        }
    }
}

class MultwinCore(var side: Side? = null){
    var life = 3
}

class Multwin(val core:MultwinCore,var flip: Boolean) : Voin{
    override val life: Int
        get() = core.life
    override val side: Side?
        get() = core.side
}