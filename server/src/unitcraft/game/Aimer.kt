package unitcraft.game

class Aimer(val exts:List<Ext>):Aim{
    val stopAim = exts.filterIsInstance<OnStopAim>()
    override fun canMove(pgFrom: Pg, pgTo: Pg):Boolean {
        return stopAim.all{!it.stopMove(pgFrom, pgTo)}
    }

    override fun canMoveForce(pgFrom: Pg, pgTo: Pg): Boolean {
        throw UnsupportedOperationException()
    }

    override fun canSell(pgFrom: Pg, pgTo: Pg): Boolean {
        return stopAim.all{!it.stopSkil(pgFrom, pgTo)}
    }

    override fun canEnforce(pgFrom:Pg,pgTo:Pg): Boolean {
        throw UnsupportedOperationException()
    }

    override fun canDmg(pgFrom: Pg, pgTo: Pg): Boolean {
        throw UnsupportedOperationException()
    }

    override fun canSkil(pgFrom: Pg, pgTo: Pg): Boolean {
        throw UnsupportedOperationException()
    }
}

interface OnStopAim {
    fun stopMove(pgFrom: Pg, pgTo: Pg) = false
    fun stopSkil(pgFrom: Pg, pgTo: Pg) = false
}