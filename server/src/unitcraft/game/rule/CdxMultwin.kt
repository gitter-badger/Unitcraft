package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.server.Side
import java.util.HashMap

class CdxMultwin(r: Resource) : CdxVoin(r) {
    val name = "multwin"
    val tlsVoin = r.tlsVoin(name)

    override fun createRules(land: Land, g: Game) = RulesVoin {
        val voins = Grid<Multwin>()
        val coreBySide = HashMap<Side, MultwinCore>()

        editAdd(12, tlsVoin.neut) {
            val core = coreBySide.getOrPut(sideVid){MultwinCore(sideVid)}
            voins[pgEdit] = Multwin(core, pgEdit.x > pgEdit.pgser.xr / 2)
        }

        ruleEditChangeAndRemove(voins)
        ruleSolid(voins)
        ruleDrawUnit(voins,g,tlsVoin,resVoin)
        ruleEfk(voins,g)
    }
}

class MultwinCore(var side: Side? = null) {
    var life = 3
}

class Multwin(val core: MultwinCore, flip: Boolean) : VoinStd(core.side,0,flip) {
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