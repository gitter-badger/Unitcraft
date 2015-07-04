package unitcraft.game

class Pile(val tp: TpPile){
    val grid = Grid<obj>()
    object obj
}

open class TpPile(val name: String, r: Resource){
    val tile:Int = r.tile(name)
}

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class TpCatapult(r:Resource):TpPile("catapult", r) {
    val tlsAkt = r.tlsAkt(name)
}