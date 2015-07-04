package unitcraft.game

import unitcraft.game.rule.Herd
import unitcraft.game.rule.Life
import unitcraft.game.rule.Place
import unitcraft.game.rule.VoinSimple
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.ArrayList

class Drawer(
        val r: ResDrawer,
        val pgser: Pgser,
        val place: Place,
        val herds: List<Herd>,
        val piles: List<Pile>,
        val pilePointControls: List<PilePointControl>,
        val stazis: Stazis
) {
    fun draw(side:Side): List<DabOnGrid> {
        val ctx = MsgDraw(Side.a)
        for (pg in pgser.pgs) {
            val tp = place.places[pg]
            ctx.drawTile(pg, r.tilesPlace[tp]!![place.fixs[pg]!![tp]!!])
        }
        for (herd in herds) for ((pg, v) in herd.grid) {
            ctx.drawTile(pg, herd.tp.tlsVoin(ctx.sideVid, v.side), if (v.flip) r.hintTileFlip else null)
            ctx.drawText(pg, v.life.value, r.hintTextLife)
            if (v.isHided) ctx.drawTile(pg, r.tileHide)
        }
        for (pile in pilePointControls) for ((pg, p) in pile.grid) ctx.drawTile(pg, pile.tp.tls(side,p.side))
        for (pile in piles) for ((pg, _) in pile.grid) ctx.drawTile(pg, pile.tp.tile)
        for ((pg, num) in stazis.grid) ctx.drawTile(pg, r.tilesStazis[num - 1])
        return ctx.dabOnGrids
    }

    fun opterTest() = Opter((TpPlace.values().map {
        r.tilesPlace[it]!!.first() } +
            herds.map { it.tp.tlsVoin.neut } +
            pilePointControls.map { it.tp.tls.neut } +
            piles.map { it.tp.tile } +
            r.tilesStazis.last()).map { Opt(DabTile(it))
    })

    private val ranges = ArrayList<Range<Int>>().init {
        val lst = listOf(TpPlace.values().size(), herds.size(), piles.size(), 1)
        var sum = 0
        for (chance in lst) {
            add((sum..sum + chance - 1))
            sum += chance
        }
    }

    private fun select(num: Int) = ranges.idxOfFirst { num in it }!!
    private fun idxRel(num: Int) = num - ranges[select(num)].start

    fun editAdd(pg: Pg, side: Side, num: Int) {
        val idx = idxRel(num)
        when (select(num)) {
            0 -> place.places[pg] = TpPlace.values()[num]
            1 -> herds[idx].grid[pg] = VoinSimple(Life(5), side, pg.x > pg.pgser.xr / 2)
            2 -> pilePointControls[idx].grid[pg] = PointControl()
            3 -> piles[idx].grid[pg] = Pile.obj
            4 -> stazis.plant(pg)
        }
    }

    fun editRemove(pg: Pg) {
        if (stazis.grid.remove(pg)) return
        for (herd in herds) if (herd.grid.remove(pg)) return
        for (pilePC in pilePointControls) if (pilePC.grid.remove(pg)) return
        for (pile in piles) if (pile.grid.remove(pg)) return
    }

    fun editChange(pg: Pg, side: Side) {
        for (herd in herds) herd.grid[pg]?.let { v ->
            when {
                v.side == Side.n -> v.side = side
                v.side == side -> v.side = side.vs
                v.side != side -> v.side = Side.n
            }
            return
        }
    }

    fun editDestroy(pg: Pg) {

    }
}

class ResDrawer(r: Resource) {
    val tilesPlace = TpPlace.values().map { it to r.tlsList(sizeFix[it]!!, it.name(), Resource.effectPlace) }.toMap()
    val tilesStazis = r.tlsList(5, "stazis")
    val tlsMove = r.tlsAktMove
    val tileHide = r.tileHide
    val hintTileFlip = r.hintTileFlip
    val hintTextLife = r.hintTextLife

    companion object {
        val sizeFix: Map<TpPlace, Int> = mapOf(
                TpPlace.forest to 4,
                TpPlace.grass to 5,
                TpPlace.hill to 1,
                TpPlace.mount to 1,
                TpPlace.sand to 4,
                TpPlace.water to 1
        )
    }
}