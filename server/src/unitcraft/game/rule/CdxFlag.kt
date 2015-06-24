package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game

class CdxFlag(r:Resource): Cdx(r){
    val name = "flag"
    val tls = r.tlsFlatControl(name)

    override fun createRules(land: Land,g: Game) = rules{
        val flats = Grid<FlatControl>()
        flats[land.pgser.pg(4, 5)] = FlatControl()


        info(10) {
            if(msg is MsgDraw)
            for ((pg, flat) in flats) {
                msg.drawTile(pg, tls(msg.side,flat.side))
            }
        }

        edit(5,tls.neut) { when(efk){
            is EfkEditAdd -> flats[efk.pg] = FlatControl()
            is EfkEditRemove -> if(flats.remove(efk.pg)!=null) efk.eat()
        }}

        endTurn(100){
            for((pg,flat) in flats)
                g.info(MsgVoin(pg)).voin?.let{
                    flat.side = it.side
                }
        }
    }
}

class FlatControl: Flat(){
    var side:Side? = null
}


