package unitcraft.game.rule

import unitcraft.game.Resource
import unitcraft.game.Stager
import unitcraft.server.Side

class Inviser(r: Resource,val stager: Stager, val hider: Hider,val drawerVoin:DrawerVoin,val editorVoin:EditorVoin,val objs: () -> Objs) {
    val tlsVoin = r.tlsVoin("inviser")

    init {
        drawerVoin.addKind(KindInviser,tlsVoin)
        editorVoin.addKind(KindInviser,tlsVoin.neut)
        stager.onEndTurn { side ->
            for (obj in objs().byKind(KindInviser).filterIsInstance<Voin>()) {
                if (obj.side == side.vs) hider.hide(obj)
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



