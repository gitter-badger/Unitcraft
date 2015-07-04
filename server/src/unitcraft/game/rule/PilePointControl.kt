package unitcraft.game.rule

import unitcraft.game.Grid
import unitcraft.game.Resource
import unitcraft.server.Side

class PointControl {
    var side = Side.n
}

open class TpPointControl(r: Resource, val name: String, val grid: () -> Grid<PointControl>) {
    val tls = r.tlsFlatControl(name)
}

class TpMine(r: Resource, grid: () -> Grid<PointControl>) : TpPointControl(r, "mine", grid)

class TpHospital(r: Resource, grid: () -> Grid<PointControl>) : TpPointControl(r, "hospital", grid)