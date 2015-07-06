package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.WeakHashMap
import kotlin.reflect.jvm.kotlin

class Enforcer(r:Resource,resDrawSimple:ResDrawSimple,grid:()->Grid<VoinSimple>):
        HelpVoin(r,resDrawSimple,"enforcer",grid){

}