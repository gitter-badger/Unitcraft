package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.injectValue

class Stazis(r: Resource) {
    val flats: () -> Flats by injectFlats()

    init {
        val tiles = r.tlsList(5, "stazis")
        injectValue<Editor>().onEdit(PriorDraw.overObj, listOf(tiles.last()), { pg, side, num -> plant(pg) }, { pg ->
            val flat = flats()[pg]
            if (flat.has<Stazis>()) {
                flat.remove<Stazis>()
                true
            } else false
        })
        injectValue<Stager>().slotTurnEnd.add(-1, this, "стазисы теряют заряд") {
            for ((flat, stazis) in flats().bothBy<Stazis>()) {
                stazis.value -= 1
                if (stazis.value == 0) flat.remove<Stazis>()
            }
        }
        injectValue<Drawer>().slotDraw.add(50, this, "рисует стазисы") {
            for ((flat, stazis) in flats().bothBy<Stazis>()) drawTile(flat.pg, tiles[stazis.value - 1])
        }

        val mover = injectValue<Mover>()
        mover.slotStopMove.add { it.pgTo.isStazis() || it.pgFrom.isStazis() }
        mover.slotStopAdd.add { pg, side -> pg.isStazis() }

        injectValue<Spoter>().slotStopSkils.add { it.pg.isStazis() }
        injectValue<Lifer>().slotStopDamage.add { it.pg.isStazis() }
        injectValue<Magic>().slotStopMagic.add { it.isStazis() }
        injectValue<Pusher>().slotStopPush.add { it.second.last().pg.plus(it.first)?.let { it.isStazis() } ?: false || it.second.any { it.pg.isStazis() } }
    }

    fun plant(pg: Pg) {
        flats()[pg].orPut { Stazis() }
    }

    private fun Pg.isStazis() = hasStazis(this)

    class Stazis : Data {
        var value = 5
    }

    fun hasStazis(pg: Pg) = flats()[pg].has<Stazis>()
}


