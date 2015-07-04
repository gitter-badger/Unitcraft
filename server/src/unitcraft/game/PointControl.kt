package unitcraft.game

import unitcraft.server.Side

class PilePointControl(val tp:TpPointControl){
    val grid = Grid<PointControl>()
}

class PointControl{
    var side = Side.n
}

open class TpPointControl(r:Resource,val name:String){
    val tls = r.tlsFlatControl(name)
}

class TpMine(r:Resource) : TpPointControl(r,"mine")

class TpHospital(r:Resource) : TpPointControl(r,"hospital")