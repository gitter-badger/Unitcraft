package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Side
import java.util.*

class Raiser(r:Resource,
             val pgser: () -> Pgser,
             val endTurn:EndTurn,
             val stazis:Stazis,
             val breedElectric: BreedElectric,
             val breedEnforcer: BreedEnforcer
) {
    val tlsMove = r.tlsAktMove
    val tlsAkt = r.tlsAkt("electric")
    fun spots(side: Side): Map<Pg, List<Sloy>> {
        return pgser().map { it to spot(it, side, it) }.filter { it.second.isNotEmpty() }.toList().toMap()
    }

    fun spot(pgSpot: Pg, side: Side, pgSrc: Pg): List<Sloy> {
        val breed = breedElectric
        val raises = ArrayList<Raise>()
        if (breed.grid()[pgSrc] != null) breed.grid()[pgSpot]?.let { voinSpot ->
            if (!voinSpot.isHided) {
                if (pgSpot !in stazis.grid()) {
                    var isOn = voinSpot.side == side// || voinSpot
                    if (endTurn.sideTurn() != side) isOn = false
                    val r = Raise(pgSpot, isOn)
                    for (pgNear in pgSpot.near) aim(pgNear, pgSpot, breed, side, r)
                    for (pgNear in pgSpot.near) {
                        if (canMove(pgSpot, pgNear, voinSpot)) r.addFn(pgNear, tlsMove){
                            move(breed.grid(),pgSpot,pgNear)
                        }
                    }
                    raises.add(r)
                }
            }
        }
        return sloys(raises)
    }

    private fun canMove(pgFrom: Pg, pgTo: Pg, voinSpot: VoinSimple): Boolean {
        return pgTo !in breedElectric.grid()
    }

    private fun move(grid: Grid<VoinSimple>, pgFrom: Pg, pgTo: Pg) {
        grid[pgFrom]?.let {            
            grid.remove(pgFrom)
            grid[pgTo] = it
            val xd = pgFrom.x - pgTo.x
            if (xd != 0) it.flip = xd > 0
        }
    }

    private fun aim(pgNear: Pg, pgSpot: Pg, breed: Breed, side: Side, r: Raise) {
        r.addFn(pgNear, tlsAkt){

        }
    }

    fun sloys(raises:List<Raise>): List<Sloy> {
        // TODO схлопнуть, если нет пересечений
        return raises.flatMap { it.sloys() }
    }
}