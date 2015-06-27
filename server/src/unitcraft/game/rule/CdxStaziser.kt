package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land

class CdxStaziser(r: Resource) : Cdx(r) {
    val name = "staziser"
    val extVoin = ExtVoin(r, name)
    val tlsAkt = r.tlsAkt(name)
    val tlsStazis = r.tlsList(5, "stazis")

    override fun createRules(land: Land, g: Game) = rules {
        val voins = ExtVoin.std()
        extVoin.createRules(this, g,voins)

        val stazis = Grid<Int>()

        fun plant(pg: Pg) {
            stazis[pg] = 5
        }

        fun decoy(pg: Pg) {
            val num = stazis[pg]!!
            if (num > 1) stazis[pg] = num - 1
            else stazis.remove(pg)
        }

        info<MsgDraw>(30) {
            for ((pg, num) in stazis) drawTile(pg, tlsStazis[num - 1])
        }

        info<MsgRaise>(0) {
            if (stazis[pgRaise] != null) isStoped = true
            if (voins.has(src)) for (pgNear in pgRaise.near)
                if (stazis[pgNear] == null) add(pgNear, tlsAkt, EfkStazisPlant(pgNear))
        }

        make<EfkStazisPlant>(0) {
            plant(pg)
        }

        stop<EfkMove>(1) {
            if (stazis[pgFrom] != null || stazis[pgTo] != null) stop()
        }

        stop<EfkEnforce>(1) {
            if (stazis[pg] != null) stop()
        }

        stop<EfkHide>(1) {
            if (stazis[pg] != null) stop()
        }

        stop<EfkSell>(1) {
            if (stazis[pg] != null) stop()
        }

        editAdd(50, tlsStazis.last()) {
            plant(pgEdit)
        }

        make<EfkEditRemove>(-50) {
            if (stazis.remove(pgEdit) != null) eat()
        }

        endTurn(10) {
            stazis.forEach { decoy(it.key) }
        }
    }
}

class EfkStazisPlant(val pg: Pg) : Efk()