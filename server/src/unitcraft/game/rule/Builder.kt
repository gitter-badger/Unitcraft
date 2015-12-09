package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpSolid
import unitcraft.server.Side
import java.util.*

class Builder(r: Resource) {
    val price = 3
    val fabriks = ArrayList<Fabrik>()
    val lifer: Lifer by inject()
    val mover: Mover by inject()
    val objs: () -> Objs by injectObjs()

    init {
        val tileAkt = r.tile("build", Resource.effectAkt)
        val hintText = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
        val hintTextRed = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'red';")
        val lifer = injectValue<Lifer>()
        val tracer = injectValue<Tracer>()
        val siklerMove = injectValue<SkilerMove>()
        injectValue<Spoter>().addSkilByBuilder<SkilBuild> {
            val data = obj<SkilBuild>()
            val opts = fabriks.map { Opt(listOf(DabTile(it.tile), DabText(price.toString(), if (obj.life >= price) hintText else hintTextRed)),obj.life >= price) }
            for (pg in data.zone(obj)) {
                val ok = mover.canAdd(pg, sideVid)
                if (ok != null) aktOpt(pg, tileAkt, opts) { num ->
                    ok{objNew ->
                        objNew.side = obj.side
                        data.fabriks[num].create(objNew)
                        data.refine(objNew)
                        tracer.touch(objNew, tileAkt)
                        lifer.poison(obj, price)
                        siklerMove.spendAll(obj)
                    }
                }
            }

        }
    }

    fun plusGold(side: Side?, value: Int) {
        objs().bothBy<SkilBuild>(side).forEach { lifer.heal(it.first, value) }
    }

    fun add(obj: Obj, refine: (Obj) -> Unit = {}, zone: (Obj) -> List<Pg> = {it.further()}) {
        obj.add(SkilBuild(zone, fabriks, refine))
        lifer.change(obj, 30)
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
        injectValue<Objer>().add(tls.neut, null, TpSolid.builder) {
            it.add(DataTileObj(tls))
            it.add(DataRedeployer())
            builder.add(it)
        }

        val tileAkt = r.tileAkt("redeployer")
        val objs = injectObjs().value
        val spoter = injectValue<Spoter>()
        val magic = injectValue<Magic>()
        spoter.addSkilByBuilder<DataRedeployer> {
            val data = obj<DataRedeployer>()
            if(data.charged) obj.near().filter { objs()[it]?.let { it.life >= 3 } ?: false && magic.canMagic(it) }.forEach {
                akt(it, tileAkt) {
                    objs()[it]?.let {
                        objs().remove(it)
                        builder.plusGold(it.side, 5)
                        data.charged = false
                    }
                }
            }
        }
        spoter.listOnTire.add { obj ->
            obj.orNull<DataRedeployer>()?.charged = true
        }
    }

    private class DataRedeployer() : Data{
        var charged = true
    }
}

class Armorer(r: Resource) {
    init {
        val solider = injectValue<Objer>()
        val builder = injectValue<Builder>()
        val lifer = injectValue<Lifer>()
        val skilerMove = injectValue<SkilerMove>()
        val tls = r.tlsVoin("armorer")
        solider.add(tls.neut, null, TpSolid.builder) {
            it.add(DataTileObj(tls))
            skilerMove.slow(it)
            builder.add(it, {
                skilerMove.slow(it)
                lifer.heal(it, 1)
            })
        }
    }
}

class Airport(r: Resource) {
    init {
        val solider = injectValue<Objer>()
        val builder = injectValue<Builder>()
        val tls = r.tlsVoin("airport")
        solider.add(tls.neut, null, TpSolid.builder, false) {
            it.add(DataTileObj(tls))
            builder.add(it, {}, { it.pg.pgser.pgs })
        }
    }
}

class Inviser(r: Resource) {
    init {
        val mover = injectValue<Mover>()
        val tls = r.tlsVoin("inviser")
        val builder = injectValue<Builder>()
        injectValue<Objer>().add(tls.neut, null, TpSolid.builder) {
            it.add(DataTileObj(tls))
            it.add(DataInviser)
            builder.add(it, { it.add(DataInviser) })
        }
        mover.slotHide.add { it.has<DataInviser>() }
    }

    private object DataInviser : Data
}