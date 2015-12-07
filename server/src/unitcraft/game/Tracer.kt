package unitcraft.game

import unitcraft.game.rule.Lifer
import unitcraft.game.rule.Obj
import unitcraft.inject.injectValue
import unitcraft.server.Side

class Tracer(r:Resource) {
    val allData by injectAllData()
    val htTurns = r.hintTilesTurn
    val htTouch = r.hintTile("ctx.translate(0.3*rTile,0);ctx.translate(0.1*rTile,-0.1*rTile);ctx.scale(0.7,0.7);")

    init {
        val tileMove = r.tile("move")
        val tileMoveJump = r.tile("move.jump")
        val tileKick = r.tile("kick")
        val tileKickJump = r.tile("kick.jump")
        injectValue<Mover>().slotMoveAfter.add { move ->
            Side.ab.forEach { side ->
                if(move.obj.isVid(side)) {
                    if (move.pgFrom.isNear(move.pgTo)) traceTile(side, move.pgFrom, if(move.isKick) tileKick else tileMove, htTurns[move.pgFrom.dr(move.pgTo)])
                    else traceTile(side, move.pgFrom, if(move.isKick) tileKickJump else tileMoveJump)
                }
            }
            false
        }
    }

    private fun traceTile(side:Side,pg: Pg, tile:Tile, hintTile:HintTile? = null) {
        traces()[side]!!.add(DabOnGrid(pg, DabTile(tile,hintTile)))
    }

    private fun traceText(side:Side,pg: Pg, text:String, hintText: HintText? = null) {
        traces()[side]!!.add(DabOnGrid(pg, DabText(text,hintText)))
    }

    private fun traceText(side:Side,pg: Pg, value:Int, hintText: HintText? = null) {
        traceText(side,pg,value.toString(),hintText)
    }

    fun traces() = allData().traces

    fun clear() {
        allData().traces.values.forEach { it.clear() }
    }

    fun touch(pg:Pg,tile:Tile){
        Side.ab.forEach { side -> traceTile(side,pg,tile,htTouch)}
    }

    fun touch(obj: Obj, tile:Tile) = touch(obj.pg,tile)

    fun trace(pg: Pg, tile:Tile, hintTile:HintTile? = null){
        println(tile)
        Side.ab.forEach { side -> traceTile(side,pg,tile,hintTile)}
    }
}