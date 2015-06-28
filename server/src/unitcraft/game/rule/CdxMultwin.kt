package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.HashMap

class CdxMultwin(r: Resource) : Cdx(r) {
    val name = "multwin"
    val extVoin = ExtVoin(r, name)

    override fun createRules(land: Land, g: Game) = rules {
        val voins = Grid<Multwin>()
        val coreBySide = HashMap<Side, MultwinCore>()
        extVoin.createRules(this, g, ExtVoin.fromGrid(voins) { side, life, flip -> Multwin(coreBySide.getOrPut(side){MultwinCore(side)}, flip) })
    }
}

class MultwinCore(var side: Side? = null) {
    var life = 3
}

class Multwin(val core: MultwinCore, override var flip: Boolean) : ExtVoin.Voins.VoinMut {
    override var life: Int
        get() = core.life
        set(v: Int) {
            core.life = v
        }

    override var side: Side?
        get() = core.side
        set(v: Side?) {
            core.side = v
        }
}