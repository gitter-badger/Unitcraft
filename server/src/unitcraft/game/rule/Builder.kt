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
        val tracer = injectValue<Tracer>()
        injectValue<Spoter>().addSkilByBuilder<SkilBuild> {
            val data = obj<SkilBuild>()
            val dabs = fabriks.map { listOf(DabTile(it.tile), DabText(price.toString(), hintText)) }
            for (pg in data.zone(obj)) mover.canAdd(pg, sideVid) { objNew, num ->
                objNew.side = obj.side
                data.fabriks[num].create(objNew)
                data.refine(objNew)
                tracer.touch(objNew,tileAkt)
                lifer.damage(obj, price)
            }?.let { aktOpt(pg, tileAkt, dabs, it) }
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
        fabriks.sort(compareBy { it.prior })
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
        spoter.addSkil<DataRedeployer>() { sideVid, obj ->
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

class Armorer(r: Resource) {
    init {
        val solider = injectValue<Solider>()
        val builder = injectValue<Builder>()
        val lifer = injectValue<Lifer>()
        val skilerMove = injectValue<SkilerMove>()
        val tls = r.tlsVoin("armorer")
        solider.add(tls.neut, null, TpSolid.builder, false) {
            it.data(DataTileObj(tls))
            builder.add(it, { it.further() }, {
                skilerMove.slow(it)
                lifer.heal(it, 1)
            })
        }
    }
}

class Airport(r: Resource) {
    init {
        val solider = injectValue<Solider>()
        val builder = injectValue<Builder>()
        val tls = r.tlsVoin("airport")
        solider.add(tls.neut, null, TpSolid.builder, false) {
            it.data(DataTileObj(tls))
            builder.add(it, { it.pg.pgser.pgs }, {})
        }
    }
}

class Inviser(r: Resource) {
    init {
        val mover = injectValue<Mover>()
        val tls = r.tlsVoin("inviser")
        val builder = injectValue<Builder>()
        injectValue<Solider>().add(tls.neut, null, TpSolid.builder) {
            it.data(DataTileObj(tls))
            it.data(DataInviser)
            builder.add(it, { it.near() }, { it.data(DataInviser) })
        }
        mover.slotHide.add { it.has<DataInviser>() }
    }

    private object DataInviser : Data
}