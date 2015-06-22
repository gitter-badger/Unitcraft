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
        spot(1){
            if (pgRaise in flats) {
                val msg = MsgRaiseCatapult(pgRaise)
                if(g.trap(msg)){
                    val r = raise(msg.isOn)
                    for (pg in g.pgs) r.add(pg, tlsAkt, EfkMove(pgRaise, pg, msg.what!!))
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

abstract class MsgRaise : Msg(){
    var isOn = false
}

class MsgRaiseCatapult(val pg:Pg) : MsgRaise(){
    var what:Any? = null
    override fun isOk() = what!=null
}