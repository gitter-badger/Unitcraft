package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game

class CdxCatapult(r:Resource): Cdx(r){
    val name = "catapult"
    val tile:Int = r.tile(name)
    val tlsAkt = r.tlsAkt(name)

    override fun createRules(land: Land,g: Game) = rules{
        val flats = Grid<Boolean>()
        flats[land.pgser.pg(3, 3)] = true


        draw(10) {
            for ((pg, flat) in flats) {
                drawTile(pg, tile)
            }
        }

        /** если воин стоит на катапульте, то дать ему способность катапульты */
        spot(1){
            val voin = g.voin(pgRaise,side)
            if (pgRaise in flats && voin!=null) {
                val r = raise(voin.side)
                for(pg in g.pgs) if(g.can(From(pgRaise).voin(voin.side),Aim(pg),TpMake.move)){
                    r.akt(pg,tlsAkt){ g.make(From(pgRaise).voin(voin.side),Aim(pg),TpMake.move) }
                }
            }
        }

        edit(5,tile) {
            when(tp){
                TpEdit.add -> flats[pgAim] = true
                TpEdit.remove -> consume(flats.remove(pgAim)!=null)
            }
        }
    }
}