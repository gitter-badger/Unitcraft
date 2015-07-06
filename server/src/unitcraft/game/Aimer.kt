package unitcraft.game

class Aimer(val exts:List<OnStopAim>):Aim{
    override fun canMove(pgFrom: Pg, pgTo: Pg):Boolean {
        return exts.all{!it.stopMove(pgFrom, pgTo)}
    }

    override fun canMoveForce(pgFrom: Pg, pgTo: Pg): Boolean {
        throw UnsupportedOperationException()
    }

    override fun canSell(pgFrom: Pg, pgTo: Pg): Boolean {
        return exts.all{!it.stopSkil(pgFrom, pgTo)}
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