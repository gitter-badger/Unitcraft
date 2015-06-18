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
    val tlsVoin = r.tlsVoin(name)
    val tlsAkt = r.tlsAkt(name)
    val tlsMove = r.tlsAktMove
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife
    val buildik = r.buildik(tlsVoin.neut)

    val tlsStazis = r.tlsList(5, "stazis")

    override fun initRules(land: Land,g: Game) = rules{
        val voins = Grid<VoinStaziser>()
        val stazis = Grid<Int>()

        fun plant(pg:Pg) {
            stazis[pg] = 5
        }

        fun decoy(pg:Pg) {
            val num = stazis[pg]
            if(num > 1) stazis[pg] = num - 1
            else stazis.remove(pg)
        }

        draw(20) {
            for ((pg,v) in voins) if(v.isVid(side)) {
                drawTile(pg, tlsVoin(side, v.side), if (v.flip) hintTileFlip else null)
                drawText(pg, v.life.toString(), hintTextLife)
            }
        }

        draw(30) {
            for ((pg,num) in stazis) drawTile(pg, tlsStazis[num-1])
        }

        spot(0) {
            if (pgRaise in voins) {
                val voin = voins[pgRaise]
                val r = raise(voin.side)
                for(pgNear in pgRaise.near) if(g.can(From(pgRaise).voin(voin.side),Aim(pgNear),TpMake.move)){
                    r.akt(pgNear,tlsMove){ g.make(From(pgRaise).voin(voin.side),Aim(pgNear),TpMake.move) }
                }
                for(pgNear in pgRaise.near) if(g.can(From(pgRaise).voin(voin.side),Aim(pgNear),TpMake.skil)){
                    r.akt(pgNear,tlsAkt){ plant(pgNear) }
                }
            }
        }

        stop(0) {
            tp == TpMake.move && voins[aim.pg] != null
        }

        stop(1) {
            stazis[from.pg]!=null || stazis[aim.pg] != null
        }

        make(0) {
            if(tp == TpMake.move) {
                val v = voins[from.pg]
                voins.remove(from.pg)
                voins[aim.pg] = v
                val xd = from.pg.x - aim.pg.x
                if (xd != 0) v.flip = xd > 0
            }
        }

        voin(10) {
            val v = voins[pg]
            if(v!=null && v.isVid(side)) put(v)
        }

        fun editChange(side: Side, pg: Pg): Boolean {
            val unt = voins[pg]
            if(unt!=null) {
                when {
                    unt.isAlly(side) -> unt.side = side.vs()
                    unt.isEnemy(side) -> unt.side = null
                    unt.isNeutral() -> unt.side = side
                }
                return true
            }else return false
        }

        edit(12,tlsVoin.neut) {
            when(tp){
                TpEdit.add -> voins[pgAim] = VoinStaziser(side,pgAim.x>pgAim.pgser.xr/2)
                TpEdit.remove -> consume(voins.remove(pgAim)!=null)
                TpEdit.change -> consume(editChange(side,pgAim))
            }
        }

        edit(50,tlsStazis.last()) {
            when(tp) {
                TpEdit.add -> plant(pgAim)
                TpEdit.remove -> consume(stazis.remove(pgAim)!=null)
            }
        }
//
//        override fun editorRem(g: Game, pg: Pg): Boolean {
//            return if (stazis[pg]!=null) stazis.remove(pg)!=null else unts.remove(pg)!=null
//        }
//
//        override fun editorDestroy(g: Game, pg: Pg): Boolean {
//            return false
//        }
//

    }
}

class VoinStaziser(override var side:Side?,var flip: Boolean): Voin {
    override var life = 3
    var hide = false
    fun isVid(side:Side) = !hide || isAlly(side)
}