package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Side
import unitcraft.server.idxOfFirst
import unitcraft.server.init
import java.util.ArrayList

class Drawer(
        val r: ResDrawer,
        val pgser: () -> Pgser,
        val place: Place,
        val breeds: List<Breed>,
        val tpPiles: List<TpPile>,
        val tpPointControls: List<TpPointControl>,
        val stazis: Stazis
) {
    fun draw(side:Side): List<DabOnGrid> {
        val ctx = MsgDraw(Side.a)
        for (pg in pgser().pgs) {
            val tp = place.grid()[pg]
            ctx.drawTile(pg, r.tilesPlace[tp]!![place.fixs()[pg][tp]!!])
        }
        for (breed in breeds) for ((pg, v) in breed.grid()) {
            ctx.drawTile(pg, breed.tlsVoin(ctx.sideVid, v.side), if (v.flip) r.hintTileFlip else null)
            ctx.drawText(pg, v.life.value, r.hintTextLife)
            if (v.isHided) ctx.drawTile(pg, r.tileHide)
        }
        for (tp in tpPointControls) for ((pg, p) in tp.grid()) ctx.drawTile(pg,tp.tls(side,p.side))
        for (pile in tpPiles) for ((pg, _) in pile.grid()) ctx.drawTile(pg, pile.tile)
        for ((pg, num) in stazis.grid()) ctx.drawTile(pg, r.tilesStazis[num - 1])
        return ctx.dabOnGrids
    }

    fun opterTest() = Opter((TpPlace.values().map {
        r.tilesPlace[it]!!.first() } +
            breeds.map { it.tlsVoin.neut } +
            tpPointControls.map { it.tls.neut } +
            tpPiles.map { it.tile } +
            r.tilesStazis.last()).map { Opt(DabTile(it))
    })

    private val ranges = ArrayList<Range<Int>>().init {
        val lst = listOf(TpPlace.values().size(), breeds.size(), tpPointControls.size(),tpPiles.size(), 1)
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
            0 -> place.grid()[pg] = TpPlace.values()[num]
            1 -> breeds[idx].grid()[pg] = VoinSimple(Life(5), side, pg.x > pg.pgser.xr / 2)
            2 -> tpPointControls[idx].grid()[pg] = PointControl()
            3 -> tpPiles[idx].grid()[pg] = TpPile.obj
            4 -> stazis.plant(pg)
        }
    }

    fun editRemove(pg: Pg) {
        if (stazis.grid().remove(pg)) return
        for (herd in breeds) if (herd.grid().remove(pg)) return
        for (pilePC in tpPointControls) if (pilePC.grid().remove(pg)) return
        for (pile in tpPiles) if (pile.grid().remove(pg)) return
    }

    fun editChange(pg: Pg, side: Side) {
        for (herd in breeds) herd.grid()[pg]?.let { v ->
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