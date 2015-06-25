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

        info<MsgDraw>(20) {
            for ((pg, v) in voins) {
                drawTile(pg, tlsVoin(sideVid, v.side), if (v.flip) hintTileFlip else null)
                drawText(pg, v.life.toString(), hintTextLife)
                if (g.info(InfoIsHide(v)).isHided) drawTile(pg, tileHide)
                g.info(MsgDrawVoin(this, pg, v))
            }
        }

        info<MsgSpot>(0) {
            voins[pgSpot]?.let{
                add(g.info(MsgRaise(g, pgSpot,it,it)))
            }
        }

        info<MsgRaise>(0) {
            if (voins.containsValue(src))
                for (pgNear in pgRaise.near) {
                    add(pgNear, tlsMove, EfkMove(pgRaise, pgNear, voinEfk))
                }
        }

        stop<EfkMove>(0) {
            if (voins[pgTo] != null) stop()
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

        make<EfkDmg>(0) {
            voins[pgAim]?.let {
                if (it == voin) it.core.life -= 1
            }
        }

        after<EfkMove>(0) {
            for (pg in pgTo.near) {
                g.info(MsgVoin(pg)).voin?.let{
                    g.make(EfkUnhide(pg,it))
                }
            }
        }

        info<MsgVoin>(0) {
             voins[pg]?.let { add(it) }
        }

        info<MsgRaise>(0) {
            isOn = voinEfk.side == g.sideTurn
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

        editAdd(12, tlsVoin.neut) {
            val core = coreBySide.getOrPut(sideVid){MultwinCore(sideVid)}
            voins[pgEdit] = Multwin(core, pgEdit.x > pgEdit.pgser.xr / 2)
        }

        make<EfkEditRemove>(-12){
            if(voins.remove(pgEdit) != null) eat()
        }
        make<EfkEditChange>(12){
            if(editChange(sideVid, pgEdit)) eat()
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