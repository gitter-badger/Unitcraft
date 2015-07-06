package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.Collections
import java.util.WeakHashMap

class Redeployer(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd{
        override val tlsVoin = r.tlsVoin("redeployer")
}
