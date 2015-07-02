package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.WeakHashMap
import kotlin.reflect.jvm.kotlin

class CdxEnforcer(r: Resource) : CdxVoin(r) {
    val name = "enforcer"
    val tlsVoin = r.tlsVoin(name)
    val tlsAkt = r.tlsAkt(name)
    val tlsEnforced = r.tlsBool("enforced", "enforcedAlready")

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()
        val enforced = WeakHashMap<Voin, Boolean>()

        ruleVoin(g,voins,resVoin,tlsVoin)

        aimByHand(g,voins,resVoin){ pg,pgRaise,voinRaise,sideVid,r ->
            if(g.info(CanGetEnforce(pg,sideVid)).can && g.info(StopSkil(pgRaise,pg)).isStoped) {
                r.addFn(pg, tlsAkt){
                    g.info(EfkEnforce(pg))
                    voinRaise.tire()
                }
            }
        }

        info<MsgTgglRaise>(0) {
            if (enforced[voinRaise]?:false) isOn = true
        }

//        info<MsgRaise>(10) {
//            if (voins.has(src)) for (pgNear in pgRaise.near) {
//                g.voin(pgNear,sideVid)?.let {
//                    add(pgNear, tlsAkt, EfkEnforce(pgNear, it))
//                }
//            }
//        }

        info<MsgDrawVoin>(10) {
            enforced[voin]?.let { drawTile(pg, tlsEnforced(it)) }
        }

        endTurn(0) {
            enforced.clear()
        }
    }
}

class EfkEnforce(val pg: Pg) : Msg()

class CanGetEnforce(val pg:Pg,val sideVid: Side) : Msg(){
    var can:Boolean = false
        private set

    fun confirm(){
        can = true
        eat()
    }
}

class StopSkil(val pgFrom:Pg,val pgTo:Pg) : Msg(){
    var isStoped:Boolean = false
        private set

    fun confirm(){
        isStoped = true
        eat()
    }
}