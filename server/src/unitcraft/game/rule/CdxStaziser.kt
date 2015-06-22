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

        draw(30) {
            for ((pg,num) in stazis) drawTile(pg, tlsStazis[num-1])
        }

        spot(0) {
            val voin = rsVoin[pgRaise]
            if(voin!=null) {
                val msg = MsgRaiseVoin(pgRaise,voin)
                if(g.trap(msg)) {
                    val r = raise(msg.isOn)
                    for (pgNear in pgRaise.near) {
                        r.add(pgNear, tlsAkt, EfkStazisPlant(pgNear))
                    }
                }
            }
        }

        make(0){
            if(efk is EfkStazisPlant) plant(efk.pg)
        }

        stop(1) { when(msg){
            is EfkMove -> if(stazis[msg.pgFrom]!=null || stazis[msg.pgTo] != null) msg.stop()
            is EfkSttAdd -> if(stazis[msg.pgTo]!=null) msg.stop()
            is EfkHide -> if(stazis[msg.pg]!=null) msg.stop()
            is EfkStazisPlant -> if(stazis[msg.pg]!=null) msg.stop()
            is MsgRaiseVoin -> if(stazis[msg.pg]!=null) msg.stop()
            is MsgRaiseCatapult -> if(stazis[msg.pg]!=null) msg.stop()
        }}

        edit(50,tlsStazis.last()) {when(efk) {
                is EfkEditAdd -> plant(efk.pg)
                is EfkEditRemove -> consume(stazis.remove(efk.pg)!=null)
        }}

        endTurn(10){
            stazis.forEach { decoy(it.key) }
        }
    }
}

class EfkStazisPlant(val pg:Pg,var target:Boolean=false) : Efk() {
    override fun isOk()=true
}