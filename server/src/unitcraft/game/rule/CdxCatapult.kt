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
        val flats = Grid<Catapult>()
        flats[land.pgser.pg(3, 3)] = Catapult


        draw(10) {
            for ((pg, flat) in flats) {
                drawTile(pg, tile)
            }
        }

        /** если воин стоит на катапульте, то дать ему способность катапульты */
        spot(20){
            if (pgRaise in flats) {
                val voin = g.info(MsgVoin(pgRaise)).voin
                if(voin!=null) {
                    val r = raise(MsgRaise(pgRaise, voin))
                    if (r != null) for (pg in g.pgs) r.add(pg, tlsAkt, EfkMove(pgRaise,pg,voin))
                }
            }
        }

        edit(5,tile) { when(efk){
            is EfkEditAdd -> flats[efk.pg] = Catapult
            is EfkEditRemove -> consume(flats.remove(efk.pg)!=null)
        }}
    }
}

abstract class Flat

object Catapult : Flat()
