package unitcraft.game.rule

import unitcraft.game.Resource
import unitcraft.game.Stager
import unitcraft.server.Side

class Inviser(voiner:Voiner, val hider: Hider,val sider: Sider,val stager: Stager,val objs: () -> Objs) {
    init {
        voiner.voinStd(KindInviser)
        stager.onEndTurn { side ->
            for (obj in objs().byKind(KindInviser)) {
                if (sider.isEnemy(obj,side)) hider.hide(obj,this)
            }
        }
    }
    private object KindInviser:Kind()
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



