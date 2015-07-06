package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.WeakHashMap
import kotlin.reflect.jvm.kotlin

class Enforcer(r:Resource,override val grid:()->Grid<VoinSimple>):OnHerd{
        override val tlsVoin = r.tlsVoin("enforcer")
}