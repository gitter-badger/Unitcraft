package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.*

class CdxElectric(r: Resource) : CdxVoin(r) {
    val name = "electric"
    val tlsVoin = r.tlsVoin(name)
    val tlsAkt = r.tlsAkt(name)
    val tileTrace = r.tile("electric.akt")
    val hintTrace = r.hintTileTouch

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()

        fun wave(pgs: HashMap<Pg, List<Voin>>,que:ArrayList<Pg>) {
            que.firstOrNull()?.let { pg ->
                que.remove(0)
                pgs[pg] = g.info(MsgVoin(pg)).all
                que.addAll(pg.near.filter { it !in pgs && g.info(MsgVoin(it)).all.isNotEmpty() })
                wave(pgs,que)
            }
        }

        fun hitElectro(pgAim:Pg,pgFrom:Pg){
            val pgs = LinkedHashMap<Pg, List<Voin>>()
            pgs[pgFrom] = emptyList()
            val que = ArrayList<Pg>()
            que.add(pgAim)
            wave(pgs,que)
            pgs.remove(pgFrom)
            pgs.forEach{ p -> p.value.forEach{ g.make(EfkDmg(p.key,it)) }}
            g.traces.add(TraceElectric(pgs.map { it.key }))
        }

        ruleVoin(g,voins,resVoin,tlsVoin)

        aimByHand(g,voins,resVoin){ pg,pgRaise,voinRaise,sideVid,r ->
            g.info(MsgVoin(pg)).voin?.let {
                if (!g.stop(EfkDmg(pg,it))) r.addFn(pg, tlsAkt){
                    hitElectro(pg,pgRaise)
                }
            }
        }
    }

    inner class TraceElectric(val pgs:List<Pg>):Trace(){
        override fun dabsOnGrid() =
            pgs.map { DabOnGrid(it,DabTile(tileTrace,hintTrace)) }

    }
}
