package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.init
import unitcraft.game.Game

class CdxStaziser(r:Resource): Cdx(r){
    val name = "staziser"
    val extVoin = ExtVoin(r,name)
    val tlsAkt = r.tlsAkt(name)
    val tlsStazis = r.tlsList(5, "stazis")

    override fun createRules(land: Land,g: Game) = rules{
        val rsVoin = extVoin.createRules(this,land,g)
        val stazis = Grid<Int>()

        fun plant(pg:Pg) {
            stazis[pg] = 5
        }

        fun decoy(pg:Pg) {
            val num = stazis[pg]!!
            if(num > 1) stazis[pg] = num - 1
            else stazis.remove(pg)
        }

        info(30) {
            if(msg is MsgDraw)
            for ((pg,num) in stazis) msg.drawTile(pg, tlsStazis[num-1])
        }

        info(0) {
            if(msg is MsgRaise) {
                if(rsVoin.voins.containsValue(msg.src)) for (pgNear in msg.pg.near)
                    if(stazis[pgNear]==null) {
                        msg.add(pgNear, tlsAkt, EfkStazisPlant(pgNear))
                    }
            }
        }

        make(0){
            if(efk is EfkStazisPlant) plant(efk.pg)
        }

        stop(1) { when(efk){
            is EfkMove -> if(stazis[efk.pgFrom]!=null || stazis[efk.pgTo] != null) efk.stop()
            is EfkEnforce -> if(stazis[efk.pg]!=null) efk.stop()
            is EfkHide -> if(stazis[efk.pg]!=null) efk.stop()
//            is MsgSpot -> if(stazis[efk.pg]!=null) efk.stop()
            is EfkSell -> if(stazis[efk.pg]!=null) efk.stop()
        }}

        edit(50,tlsStazis.last()) {when(efk) {
                is EfkEditAdd -> plant(efk.pg)
                is EfkEditRemove -> if(stazis.remove(efk.pg)!=null) efk.eat()
        }}

        endTurn(10){
            stazis.forEach { decoy(it.key) }
        }
    }
}

class EfkStazisPlant(val pg:Pg) : Efk()