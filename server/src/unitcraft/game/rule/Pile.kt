package unitcraft.game.rule

import unitcraft.game.Grid
import unitcraft.game.Resource

open class TpPile(val name: String, r: Resource,val grid :() -> Grid<TpPile.obj>){
    val tile:Int = r.tile(name)
    object obj
}

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class TpCatapult(r: Resource,grid :() -> Grid<TpPile.obj>):TpPile("catapult", r,grid) {
    val tlsAkt = r.tlsAkt(name)
}