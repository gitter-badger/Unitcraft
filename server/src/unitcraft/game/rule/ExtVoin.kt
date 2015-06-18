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

    fun createRules(r: Rules, land: Land, g: Game): Voins {
        val voins = Voins(land, g)
        r.rules.addAll(voins.rs)
        return voins
    }

    inner class Voins(land: Land, g: Game) {
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

            spot(0) {
                val voin = voins[pgRaise]
                if(voin!=null) {
                    val r = raise(voin.side)
                    for (pgNear in pgRaise.near) if (g.can(From(pgRaise).voin(voin.side), Aim(pgNear), TpMake.move)) {
                        r.akt(pgNear, tlsMove) { g.make(From(pgRaise).voin(voin.side), Aim(pgNear), TpMake.move) }
                    }
                }
            }

            stop(0) {
                tp == TpMake.move && voins[aim.pg] != null
            }

            make(0) {
                if (tp == TpMake.move) {
                    val v = voins[from.pg]
                    if (v != null) {
                        voins.remove(from.pg)
                        voins[aim.pg] = v
                        val xd = from.pg.x - aim.pg.x
                        if (xd != 0) v.flip = xd > 0
                    }
                }
            }

            voin(10) {
                val v = voins[pg]
                if (v != null && v.isVid(side)) put(v)
            }

            fun editChange(side: Side, pg: Pg): Boolean {
                val unt = voins[pg]
                if (unt != null) {
                    when {
                        unt.isAlly(side) -> unt.side = side.vs()
                        unt.isEnemy(side) -> unt.side = null
                        unt.isNeutral() -> unt.side = side
                    }
                    return true
                } else return false
            }

            edit(12, tlsVoin.neut) {
                when (tp) {
                    TpEdit.add -> voins[pgAim] = VoinStd(side, pgAim.x > pgAim.pgser.xr / 2)
                    TpEdit.remove -> consume(voins.remove(pgAim) != null)
                    TpEdit.change -> consume(editChange(side, pgAim))
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