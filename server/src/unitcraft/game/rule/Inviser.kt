package unitcraft.game.rule

import unitcraft.game.Stager
import unitcraft.game.rule.Kind.inviser
import unitcraft.server.Side

class Inviser(voiner: Voiner, val stager: Stager, val hider: Hider, val objs: () -> Objs) {

    init {
        voiner.reg(inviser)
        stager.onEndTurn { side ->
            for (obj in objs().byKind(inviser)) {
                if (sider.side(obj) == side.vs) hider.hide(obj)
            }
        }
    }

    //    val hide : MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd,Boolean>())

    //    make<EfkUnhide>(0) {
    //        //voins[pg]?.let{hide.remove(it)}
    //    }
    //
    //    info<MsgIsHided>(0){
    //        if(voin in hide) yes()
    //    }
    //


}

