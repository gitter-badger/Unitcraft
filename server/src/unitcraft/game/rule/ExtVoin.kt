package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side

class ExtVoin(r: Resource, name: String) {
    val tlsVoin = r.tlsVoin(name)
    val tlsMove = r.tlsAktMove
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val buildik = r.buildik(tlsVoin.neut)

    fun createRules(r: Rules, land: Land, g: Game): Ruleset {
        val rs = Ruleset(land, g)
        r.rules.addAll(rs.rs)
        return rs
    }

    inner class Ruleset(land: Land, g: Game) {
        private val voins = Grid<VoinStd>()

        fun get(pg:Pg) = voins[pg]

        val rs = rules {
            println("CdxVoin.initRules")

            draw(20) {
                for ((pg, v) in voins) if (v.isVid(side)) {
                    drawTile(pg, tlsVoin(side, v.side), if (v.flip) hintTileFlip else null)
                    drawText(pg, v.life.toString(), hintTextLife)
                }
            }

            spot(10) {
                val voin = voins[pgRaise]
                if(voin!=null) {
                    val r = raise(voin.side)
                    for (pgNear in pgRaise.near) {
                        r.add(pgNear,tlsMove,EfkMove(pgRaise,pgNear,voin.side))
                    }
                }
            }

            stop(0) {
                efk is EfkMove && voins[efk.pgTo] != null
            }

            make(0) { when (efk) {
                    is EfkMove -> voins[efk.pgFrom]?.let {
                        voins.remove(efk.pgFrom)
                        voins[efk.pgTo] = it
                        val xd = efk.pgFrom.x - efk.pgTo.x
                        if (xd != 0) it.flip = xd > 0
                    }
                    is EfkHide -> voins[efk.pg]?.let {
                        if(it.side == efk.side) it.hide = true
                    }
            }}

            trap(0) { when (efk) {
                is EfkMove -> ok(efk.pgFrom in voins)
                is EfkHide -> voins[efk.pg]?.let{
                    ok(it.side == efk.side)
                }
                is EfkSttAdd -> ok(efk.pgTo in voins)
            }}

            voin(10) {
                val v = voins[pg]
                if (v != null && v.isVid(side)) put(v)
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
    var hide = false
    fun isVid(side: Side) = !hide || isAlly(side)
}

/** Самостоятельное перемещение юнита со стороной [side] */
class EfkMove(val pgFrom:Pg,val pgTo:Pg,val side:Side?) : Efk()

class EfkHide(val pg:Pg,val side:Side) : Efk()

abstract class Stt

class EfkSttAdd(val stt:Stt,val pgTo:Pg) : Efk()