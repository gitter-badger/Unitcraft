package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land

class CdxStaziser(r: Resource) : CdxVoin(r) {
    val name = "staziser"
    val tlsAkt = r.tlsAkt(name)
    val tlsVoin = r.tlsVoin(name)
    val tlsStazis = r.tlsList(5, "stazis")

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<VoinStd>()
        val stazis = Grid<Int>()

        fun plant(pg: Pg) {
            stazis[pg] = 5
        }

        fun decoy(pg: Pg) {
            val num = stazis[pg]!!
            if (num > 1) stazis[pg] = num - 1
            else stazis.remove(pg)
        }

        ruleVoin(g,voins,resVoin,tlsVoin)

        aimByHand(g,voins,resVoin) { pg,pgRaise,voinRaise,sideVid,r ->
            if (stazis[pg] == null) r.add(pg, tlsAkt, EfkStazisPlant(pg))
        }

        info<MsgDraw>(30) {
            for ((pg, num) in stazis) drawTile(pg, tlsStazis[num - 1])
        }

        info<MsgTgglRaise>(0) {
            if (stazis[pgRaise] != null) cancel()
        }

        make<EfkStazisPlant>(0) {
            plant(pg)
        }

        stop<EfkMove>(1) {
            if (stazis[pgFrom] != null || stazis[pgTo] != null) stop()
        }

         stop<EfkHide>(1) {
            if (stazis[pg] != null) stop()
        }

        stop<EfkSell>(1) {
            if (stazis[pg] != null) stop()
        }

        stop<EfkDmg>(1) {
            if (stazis[pgAim] != null) stop()
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