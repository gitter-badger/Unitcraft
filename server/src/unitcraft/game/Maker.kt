package unitcraft.game

class Maker(val exts: List<Ext>){
    val makes = exts.filterIsInstance<OnMake>()
    val makeAfters = exts.filterIsInstance<OnMakeAfter>()
    val makeBefores = exts.filterIsInstance<OnMakeBefore>()

    fun move(pgFrom: Pg, pgTo: Pg) {

        if(makeBefores.any{it.beforeMove(pgFrom,pgTo)}) return
        makes.forEach { it.move(pgFrom, pgTo) }
        makeAfters.forEach { it.afterMove(pgFrom, pgTo) }
    }

    fun minusEnergy(pg: Pg, value: Int) {
        throw UnsupportedOperationException()
    }
}

interface OnMake{
    fun move(pgFrom: Pg, pgTo: Pg)

}

interface OnMakeAfter{
    fun afterMove(pgFrom: Pg, pgTo: Pg)
}

interface OnMakeBefore{
    fun beforeMove(pgFrom: Pg, pgTo: Pg):Boolean
}