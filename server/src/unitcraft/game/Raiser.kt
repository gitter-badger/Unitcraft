package unitcraft.game

import unitcraft.game.rule.*
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Raiser(val pgser: () -> Pgser,
             val aimer:Aimer,
             val stager: Stager,
             val exts: List<Ext>
) {
    val onRaises = exts.filterIsInstance<OnRaise>()
    fun spots(side: Side): Map<Pg,List<Sloy>> {
//        val isOn = stager.sideTurn() == side
//        return onRaises.flatMap{ ext -> ext.focus().map{
//            it.first to Rais(ext.raise(aimer,it.first,it.first,it.second),it.first,isOn).sloys()
//        }}.toMap().filter { it.value.isNotEmpty() }
        // 1) sideVid==sideFocus умолчание 2) sideTurn == sideVid 3) enforced разрешает
//        if (breed.grid()[pgSrc] != null) breed.grid()[pgSpot]?.let { voinSpot ->
//            if (!voinSpot.isHided) {
//                if (pgSpot !in stazis.grid()) {
//                    var isOn = voinSpot.side == side// || voinSpot
//                    if (endTurn.sideTurn() != side) isOn = false
//                    val r = Raise(pgSpot, isOn)
//                    for (pgNear in pgSpot.near) aim(pgNear, pgSpot, breed, side, r)
//                    for (pgNear in pgSpot.near) {
//                        if (canMove(pgSpot, pgNear, voinSpot)) r.addFn(pgNear, tlsMove){
//                            move(breed.grid(),pgSpot,pgNear)
//                        }
//                    }
//                    raises.add(r)
//                }
//            }
//        }
        return emptyMap()
    }

    private fun canMove(pgFrom: Pg, pgTo: Pg, voinSpot: VoinSimple): Boolean {
        //return pgTo !in electric.grid()
        return true
    }

    private fun move(grid: Grid<VoinSimple>, pgFrom: Pg, pgTo: Pg) {
        grid[pgFrom]?.let {            
            grid.remove(pgFrom)
            grid[pgTo] = it
            val xd = pgFrom.x - pgTo.x
            if (xd != 0) it.flip = xd > 0
        }
    }

    fun sloys(raises:List<Raise>): List<Sloy> {
        // TODO схлопнуть, если нет пересечений
        return raises.flatMap { it.sloys() }
    }
}

interface OnRaise:Ext{
    fun focus():List<Pair<Pg,Side>>
    fun raise(aim:Aim,pg:Pg,pgSrc:Pg,side:Side,r:Raise)
}

interface Aim{
    fun canMove(pgFrom:Pg,pgTo:Pg):Boolean
    fun canMoveForce(pgFrom:Pg,pgTo:Pg):Boolean
    fun canSkil(pgFrom:Pg,pgTo:Pg):Boolean
    fun canSell(pgFrom:Pg,pgTo:Pg):Boolean
    fun canEnforce(pgFrom:Pg,pgTo:Pg):Boolean
    fun canDmg(pgFrom:Pg,pgTo:Pg):Boolean
}

interface Make{
    fun move(pgFrom:Pg,pgTo:Pg)
    fun minusEnergy(pg:Pg,value:Int = 1)
    //fun moveForce()
}

class Raise(val pgRaise:Pg,val isOn:Boolean) {
    private val listSloy = ArrayList<Sloy>()

    fun add(pgAkt: Pg, tlsAkt: TlsAkt, fn: (Make) -> Unit) {
        addAkt(Akt(pgAkt, tlsAkt(isOn),  null, fn))
    }
    //    fun akt(pgAim: Pg, tlsAkt: TlsAkt, opter: Opter) = addAkt(Akt(pgAim, tlsAkt(isOn), null, opter))

    private fun addAkt(akt: Akt) {
        if (akt.pgAim == pgRaise) throw Err("self-cast not implemented: akt at ${akt.pgAim}")
        val idx = listSloy.indexOfFirst { it.aktByPg(akt.pgAim) != null } + 1
        if (idx == listSloy.size()) listSloy.add(Sloy(isOn))
        listSloy[idx].akts.add(akt)
    }

    fun sloys(): List<Sloy> {
        // TODO заполнить пустоты сверху снизу
        return listSloy
    }
}