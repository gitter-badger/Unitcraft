package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpSolid
import unitcraft.server.Side
import java.util.*

class Builder(r: Resource) {
    val price = 5
    val fabriks = ArrayList<Fabrik>()
    val lifer: Lifer by inject()
    val mover: Mover by inject()
    val objs: () -> Objs by injectObjs()

    init {
        val tileAkt = r.tile("build", Resource.effectAkt)
        val hintText = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
        val lifer = injectValue<Lifer>()
        injectValue<Spoter>().addSkil<SkilBuild> { side, obj, objSrc ->
            val data = objSrc<SkilBuild>()
            val dabs = fabriks.map { listOf(DabTile(it.tile), DabText(price.toString(), hintText)) }
            val akts = ArrayList<AktOpt>()
            for (pg in data.zone(obj)) {
                val can = mover.canBuild(Singl(pg), side)
                if (can != null) {
                    akts.add(AktOpt(pg, tileAkt, dabs) {
                        if (can()) {
                            val objCreated = Obj(Singl(pg))
                            objCreated.side = obj.side
                            data.fabriks[it].create(objCreated)
                            objs().add(objCreated)
                            data.refine(objCreated)
                            lifer.damage(obj, price)
                        }
                    })
                }
            }
            akts
        }
    }

    fun plusGold(side: Side, value: Int) {
        objs().bySide(side).by<SkilBuild, Obj>().forEach { lifer.heal(it.first, value) }
    }

    fun add(obj: Obj, zone: (Obj) -> List<Pg>, refine: (Obj) -> Unit) {
        obj.data(SkilBuild(zone, fabriks, refine))
        lifer.change(obj, 50)
    }

    fun addFabrik(prior: Int, tile: Tile, create: (Obj) -> Unit) {
        fabriks.add(Fabrik(prior, tile, create))
        fabriks.sortedBy { it.prior }
    }

    inner class SkilBuild(val zone: (Obj) -> List<Pg>, val fabriks: List<Fabrik>, val refine: (Obj) -> Unit) : Data

    class Fabrik(val prior: Int, val tile: Tile, val create: (Obj) -> Unit)
}

class Redeployer(r: Resource) {
    init {
        val tls = r.tlsVoin("redeployer")
        val builder = injectValue<Builder>()
        injectValue<Solider>().add(tls.neut, null, TpSolid.builder) {
            it.data(DataTileObj(tls))
            it.data(DataRedeployer)
            builder.add(it, { it.near() }, {})
        }

        val tileAkt = r.tileAkt("redeployer")
        val objs = injectObjs().value
        val spoter = injectValue<Spoter>()
        spoter.addSkil<DataRedeployer>() { sideVid, obj, data ->
            obj.near().filter { objs()[it]?.let { it.life >= 3 } ?: false }.map {
                AktSimple(it, tileAkt) {
                    objs()[it]?.let {
                        objs().remove(it)
                        builder.plusGold(it.side, 5)
                        spoter.tire(obj)
                    }
                }
            }
        }
    }

    object DataRedeployer : Data
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