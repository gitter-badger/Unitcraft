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

        info<MsgSpot>(20) {
            if (pgSrc in flats) g.voin(pgSpot,side)?.let {
                val tggl = g.info(MsgTgglRaise(pgSpot, it))
                if(!tggl.isCanceled) {
                    val r = Raise(pgSpot, tggl.isOn)
                    for (pg in g.pgs) if(!g.stop(EfkMove(pgSpot, pg, it))) r.add(pg, tlsAkt, EfkMove(pgSpot, pg, it))
                    add(r)
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

abstract class Flat

object Catapult : Flat()
