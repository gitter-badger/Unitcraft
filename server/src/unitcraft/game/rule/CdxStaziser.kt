package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game
import unitcraft.game.Voin

class CdxStaziser(r:Resource): Cdx(r){
    val name = "staziser"
    val extVoin = ExtVoin(r,name)
    val tlsAkt = r.tlsAkt(name)
    val tlsStazis = r.tlsList(5, "stazis")

    override fun createRules(land: Land,g: Game) = rules{
        val voins = extVoin.createRules(this,land,g)
        val stazis = Grid<Int>()

        fun plant(pg:Pg) {
            stazis[pg] = 5
        }

        fun decoy(pg:Pg) {
            val num = stazis[pg]!!
            if(num > 1) stazis[pg] = num - 1
            else stazis.remove(pg)
        }

        draw(30) {
            for ((pg,num) in stazis) drawTile(pg, tlsStazis[num-1])
        }

        spot(0) {
            val voin = voins[pgRaise]
            if(voin!=null) {
                val r = raise(voin.side)
                for (pgNear in pgRaise.near) if (g.can(From(pgRaise).voin(voin.side), Aim(pgNear), TpMake.skil)) {
                    r.akt(pgNear, tlsAkt) { plant(pgNear) }
                }
            }
        }

        stop(1) {
            stazis[from.pg]!=null || stazis[aim.pg] != null
        }

        edit(50,tlsStazis.last()) {
            when(tp) {
                TpEdit.add -> plant(pgAim)
                TpEdit.remove -> consume(stazis.remove(pgAim)!=null)
            }
        }

        endTurn(10){
            stazis.forEach { decoy(it.key) }
        }
    }
}