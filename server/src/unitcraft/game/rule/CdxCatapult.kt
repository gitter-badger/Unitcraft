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


        info<MsgDraw>(10, {
            for ((pg, flat) in flats)
                drawTile(pg, tile)
        })

        /** если воин стоит на катапульте, то дать ему способность катапульты */
        info<MsgRaise>(20){
            if(src is Catapult) for (pg in g.pgs) add(pg, tlsAkt, EfkMove(pgRaise,pg, voinRaise))
        }

        info<MsgSpot>(20) {
            if (pgSpot in flats) g.voin(pgSpot,side)?.let {
                raise(pgSpot, it, Catapult)
            }
        }

        editAdd(5,tile) {
            flats[pgEdit] = Catapult
        }

        make<EfkEditRemove>(-5){
            if(flats.remove(pgEdit)!=null) eat()
        }
    }
}

abstract class Flat : Obj

object Catapult : Flat()
