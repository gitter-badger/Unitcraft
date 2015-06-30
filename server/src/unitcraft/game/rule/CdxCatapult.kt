package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
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

        info<MsgSpot>(20) {
            if (pgSpot in flats) g.voin(pgSpot,side)?.let {
                val pgRaise = pgSpot
                val tggl = g.info(MsgTgglRaise(pgRaise, it))
                if(!tggl.isCanceled) {
                    val r = Raise(pgSpot, tggl.isOn)
                    for (pg in g.pgs) r.add(pg, tlsAkt, EfkMove(pgRaise, pg, it))
                }
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
