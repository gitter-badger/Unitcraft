package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game

class CdxMine(r:Resource): Cdx(r){
    val name = "mine"
    val tls = r.tlsFlatControl(name)

    override fun createRules(land: Land,g: Game) = rules{
        val flats = Grid<FlatControl>()
        flats[land.pgser.pg(4, 6)] = FlatControl()


        info<MsgDraw>(10) {
            for ((pg, flat) in flats) {
//                drawTile(pg, tls(sideVid,flat.side))
            }
        }

        endTurn(100){
            for((pg,flat) in flats)
                g.info(MsgVoin(pg)).voin?.let{
                    flat.side = it.side!!
                }
            for((pg,flat) in flats) flat.side?.let{
                if(it == g.sideTurn) g.make(EfkGold(1,it))
            }
        }
    }
}


