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


        info(10) {
            if(msg is MsgDraw) for ((pg, flat) in flats)
                msg.drawTile(pg, tile)
        }

        /** если воин стоит на катапульте, то дать ему способность катапульты */
        info(20){ when(msg){
            is MsgRaise -> {
                if(msg.src is Catapult) for (pg in g.pgs) msg.add(pg, tlsAkt, EfkMove(msg.pg,pg,msg.voinEfk))
            }
            is MsgSpot ->{
                if (msg.pg in flats) {
                    g.info(MsgVoin(msg.pg)).voin?.let {
                        msg.add(g.info(MsgRaise(g, msg.pg, Catapult, it)))
                    }
                }
            }
        }}

        edit(5,tile) { when(efk){
            is EfkEditAdd -> flats[efk.pg] = Catapult
            is EfkEditRemove -> if(flats.remove(efk.pg)!=null) efk.eat()
        }}
    }
}

abstract class Flat : Obj

object Catapult : Flat()
