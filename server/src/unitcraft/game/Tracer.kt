package unitcraft.game

import unitcraft.game.rule.Lifer
import unitcraft.inject.injectValue
import unitcraft.server.Side

class Tracer(r:Resource) {
    val allData by injectAllData()
    val htTurns = r.hintTilesTurn

    init {
        val tileMove = r.tile("trace.move")
        val tileJump = r.tile("trace.jump")
        injectValue<Mover>().slotMoveAfter.add { move ->
            Side.ab.forEach { side ->
                if(move.obj.isVid(side)) {
                    if (move.pgFrom.isNear(move.pgTo)) traceTile(side, move.pgFrom, tileMove, htTurns[move.pgFrom.dr(move.pgTo)])
                    else traceTile(side, move.pgFrom, tileJump)
                }
            }
            false
        }
        val tileDamage = r.tile("trace.damage")
        val tileHeal = r.tile("trace.heal")
        val hintTextDamage = r.hintText("setSizeFont(ctx,rTile/2);ctx.fillStyle = 'hotpink';ctx.translate((rTile-xrText(ctx,txt))/2,(rTile-yrText(ctx))/2);")
        injectValue<Lifer>().slotAfterDamage.add{ dmgs ->
            dmgs.forEach { dmg ->
                Side.ab.forEach { side ->
                    traceTile(side,dmg.obj.pg, if (dmg.value > 0) tileDamage else tileHeal)
                    traceText(side,dmg.obj.pg, Math.abs(dmg.value),hintTextDamage)
                }
            }
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
}