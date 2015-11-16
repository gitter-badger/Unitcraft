package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpSolid
import unitcraft.server.Side
import java.util.*

class Builder(r: Resource) {
    val tlsAkt = TlsAkt(r.tile("build", Resource.effectAkt), r.tile("build", Resource.effectAktOff))
    val hintText = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
    val price = 5
    val fabriks = ArrayList<Fabrik>()
    val lifer: Lifer by inject()
    val spoter: Spoter by inject()
    val mover: Mover by inject()
    val objs: () -> Objs by injectObjs()

    fun plusGold(side: Side, value: Int) {
        objs().bySide(side).by<SkilBuild, Obj>().forEach { lifer.heal(it.first, value) }
    }

    fun add(obj: Obj, zone: (Obj) -> List<Pg>, refine: (Obj) -> Unit) {
        obj.data(SkilBuild(zone, fabriks))
        lifer.change(obj, 50)
    }

    fun addFabrik(prior: Int, tile: Tile, create: (Obj) -> Unit) {
        fabriks.add(Fabrik(prior, tile, create))
        fabriks.sortedBy { it.prior }
    }

    inner class SkilBuild(val zone: (Obj) -> List<Pg>, val fabriks: List<Fabrik>) : Skil {
        override fun akts(sideVid: Side, obj: Obj): List<Akt> {
            val dabs = fabriks.map { listOf(DabTile(it.tile), DabText(price.toString(), hintText)) }
            val akts = ArrayList<AktOpt>()
            for (pg in zone(obj)) {
                val can = mover.canBulid(Singl(pg), sideVid)
                if (can != null) {
                    akts.add(AktOpt(pg, tlsAkt, dabs) {
                        if (can()) {
                            val objCreated = Obj(Singl(pg))
                            objCreated.side = obj.side
                            fabriks[it].create(objCreated)
                            objs().add(objCreated)
                            lifer.damage(obj, price)
                        }
                    })
                }
            }
            return akts
        }
    }

    class Fabrik(val prior: Int, val tile: Tile, val create: (Obj) -> Unit)
}

class Redeployer(r: Resource) {
    val tlsAkt = r.tlsAkt("redeployer")
    val solider: Solider by inject()
    val spoter: Spoter by inject()
    val builder: Builder by inject()
    val objs: () -> Objs by injectObjs()

    init {
        val tls = r.tlsVoin("redeployer")
        val skil = SkilRedeployer()
        solider.add(tls.neut,null, TpSolid.builder, false) {
            solider.addTls(it, tls)
            it.data(skil)
            builder.add(it, { it.near() }, {})
        }
    }

    inner class SkilRedeployer() : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.near().filter { objs()[it]?.let { it.life >= 3 } ?: false }.map {
                    AktSimple(it, tlsAkt) {
                        objs()[it]?.let {
                            objs().remove(it)
                            builder.plusGold(it.side, 5)
                            spoter.tire(obj)
                        }
                    }
                }
    }
}

class Warehouse(r: Resource) {
    init {
        val solider = injectValue<Solider>()
        val builder = injectValue<Builder>()
        val lifer = injectValue<Lifer>()
        val tls = r.tlsVoin("warehouse")
        solider.add(tls.neut, null, TpSolid.builder, false) {
            solider.addTls(it, tls)
            builder.add(it, { it.further() }, { lifer.heal(it, 2) })
        }
    }
}