package unitcraft.game

class Maker(
        val makes:List<OnMake>,
        val makeAfters:List<OnMakeAfter>,
        val makeBefores:List<OnMakeBefore>
):Make{
    override fun move(pgFrom: Pg, pgTo: Pg) {
        if(makeBefores.any{it.beforeMove(pgFrom,pgTo)}) return
        makes.forEach { it.move(pgFrom, pgTo) }
        makeAfters.forEach { it.afterMove(pgFrom, pgTo) }
    }

    override fun minusEnergy(pg: Pg, value: Int) {
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