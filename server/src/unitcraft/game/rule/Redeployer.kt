package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class Redeployer(r:Resource,resDrawSimple:ResDrawSimple,grid:()->Grid<VoinSimple>):
        HelpVoin(r,resDrawSimple,"redeployer",grid){

}
