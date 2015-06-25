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

        info<MsgDraw>(10) {
            for ((pg, flat) in flats) {
                drawTile(pg, tls(sideVid,flat.side))
            }
        }

        editAdd(5,tls.neut) {
            flats[pgEdit] = FlatControl()
        }

        make<EfkEditRemove>(5){
            if(flats.remove(pgEdit)!=null) eat()
        }

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


