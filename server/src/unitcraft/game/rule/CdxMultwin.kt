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
            if (msg is MsgRaise) if (voins.containsValue(msg.src))
                for (pgNear in msg.pg.near) {
                    msg.add(pgNear, tlsMove, EfkMove(msg.pg, pgNear, msg.voinEfk))
                }
        }

        stop(0) {
            if (efk is EfkMove && voins[efk.pgTo] != null) efk.stop()
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
                    if (it == efk.voin) it.core.life -= 1
                }
            }
        }

        after(0) {
            when (efk) {
                is EfkMove -> for (pg in efk.pgTo.near) {
                    g.info(MsgVoin(pg)).voin?.let{
                        g.make(EfkUnhide(pg,it))
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
                    v.isAlly(side) -> v.core.side = side.vs()
                    v.isEnemy(side) -> v.core.side = null
                    v.side == null -> v.core.side = side
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
                is EfkEditRemove -> if(voins.remove(efk.pg) != null) efk.eat()
                is EfkEditChange -> if(editChange(efk.side, efk.pg)) efk.eat()
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