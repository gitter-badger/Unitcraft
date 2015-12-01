package unitcraft.game.rule

import unitcraft.game.Drawer
import unitcraft.game.PriorDraw
import unitcraft.game.Resource
import unitcraft.inject.injectValue

class DrawerObj(r: Resource){
    val tileFlatNull = r.tile("null.flat")
    init{
        injectValue<Drawer>().onDraw(PriorDraw.obj){ side,ctx ->

        }
    }

    fun setTls(obj:Obj){

    }


}
