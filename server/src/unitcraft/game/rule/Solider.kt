package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpSolid
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*

class Solider(r: Resource) {
    private val hintTileFlipHide = r.hintTile("ctx.translate(rTile,0);ctx.scale(-1,1);ctx.globalAlpha=0.7;")
    private val hintTileFlip = r.hintTile("ctx.translate(rTile,0);ctx.scale(-1,1);")
    private val hintTileHide = r.hintTile("ctx.globalAlpha=0.7;")
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")

    private val tileHide = r.tile("hide")
    val tilesEditor = ArrayList<Tile>()
    val creates = ArrayList<(Obj) -> Unit>()
    private val landTps = HashMap<TpSolid, MutableList<(Obj) -> Unit>>()
    val drawer: Drawer by inject()
    val editor: Editor by inject()
    val mover: Mover  by inject()
    val builder: Builder by inject()
    val skilerMove: SkilerMove by inject()
    val objs: () -> Objs by injectObjs()

    init {
        editor.onEdit(PriorDraw.obj, tilesEditor, { pg, side, num ->
            val shape = Singl(pg)
            objs().byClash(shape).forEach { objs().remove(it) }
            val obj = Obj(shape)
            obj.flip = pg.x > pg.pgser.xr / 2
            obj.side = side
            obj.isFresh = true
            creates[num](obj)
            objs().add(obj)
        }, { pg ->
            objs()[pg]?.let {
                objs().remove(it)
            } ?: false
        })
        drawer.drawObjs.add { obj, side, ctx ->
            if (!obj.isVid(side)) ctx.drawTile(obj.head(), tileHide)
        }
        mover.slotMoveAfter.add { shapeFrom, move ->
            val d = shapeFrom.head.x - move.shapeTo.head.x
            if (d != 0) move.obj.flip = d > 0
        }
    }

    fun add(tile: Tile, tpSolid: TpSolid? = null, isFabric: Boolean = true, hasMove: Boolean = true, create: (Obj) -> Unit) {
        tilesEditor.add(tile)
        val crt = if (hasMove) { obj -> create(obj); skilerMove.add(obj, 3) } else create
        creates.add(crt)
        if (isFabric) builder.addFabrik(0, tile, crt)
        if (tpSolid != null) landTps.getOrPut(tpSolid) { ArrayList<(Obj) -> Unit>() }.add(create)
    }

    fun addTls(obj: Obj, tlsSolid: TlsSolid) {
        obj.data(HasTlsSolid(tlsSolid))
    }

    fun editChange(pg: Pg, sideVid: Side) {
        objs()[pg]?.let {
            it.side = when (it.side) {
                Side.n -> sideVid
                sideVid -> sideVid.vs
                sideVid.vs -> Side.n
                else -> throw Err("unknown side=${it.side}")
            }
        }
    }

    fun maxFromTpSolid() = landTps.mapValues { it.value.size }

    fun reset(solidsL: ArrayList<unitcraft.land.Solid>) {
        for (solidL in solidsL) {
            val solid = Obj(solidL.shape)
            landTps[solidL.tpSolid]!![solidL.num](solid)
            objs().add(solid)
        }
    }

    inner class HasTlsSolid(val tlsSolid: TlsSolid) : HasTileObj {
        override fun tile(sideVid: Side, obj: Obj) = tlsSolid(sideVid, obj.side, obj.isFresh)

        override fun hint(sideVid: Side, obj: Obj): Int? {
            val hided = !obj.isVid(sideVid.vs)
            return when {
                obj.flip && hided -> hintTileFlipHide
                obj.flip -> hintTileFlip
                hided -> hintTileHide
                else -> null
            }
        }
    }
}

class Telepath(r: Resource) {
    init {
        val enforcer = injectValue<Enforcer>()
        val solider = injectValue<Solider>()
        val spoter = injectValue<Spoter>()
        val tls = r.tlsVoin("telepath")
        val tlsAkt = r.tlsAkt("telepath")
        val skil = SkilTelepath(enforcer, spoter, tlsAkt)
        solider.add(tls.neut) {
            solider.addTls(it, tls)
            it.data(skil)
        }
    }

    class SkilTelepath(val enforcer: Enforcer, val spoter: Spoter, val tlsAkt: TlsAkt) : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.near().filter { enforcer.canEnforce(it, sideVid) }.map {
                    AktSimple(it, tlsAkt) {
                        enforcer.enforce(it)
                        spoter.tire(obj)
                    }
                }
    }
    //    fun spot() {
    //        for (pgNear in pgSpot.near)
    //            if (enforcer.canEnforce(pgNear)) {
    //                s.add(pgNear, tlsAkt) {
    //                    enforcer.enforce(pgNear)
    //                    voin.energy = 0
    //                }
    //            }
    //
    //    }


}

class Staziser(r: Resource) {
    init {
        val stazis = injectValue<Stazis>()
        val solider = injectValue<Solider>()
        val spoter = injectValue<Spoter>()
        val tls = r.tlsVoin("staziser")
        val tlsAkt = r.tlsAkt("staziser")
        solider.add(tls.neut) {
            solider.addTls(it, tls)
            it.data(SkilStaziser(tlsAkt, stazis, spoter))
        }
    }

    class SkilStaziser(val tlsAkt: TlsAkt, val stazis: Stazis, val spoter: Spoter) : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.near().filter { it !in stazis }.map {
                    AktSimple(it, tlsAkt) {
                        stazis.plant(it)
                        spoter.tire(obj)
                    }
                }
    }
}

class Inviser(r: Resource) {
    init {
        val solider = injectValue<Solider>()
        val mover = injectValue<Mover>()
        val skilerHit = injectValue<SkilerHit>()
        val tls = r.tlsVoin("inviser")
        solider.add(tls.neut) {
            solider.addTls(it, tls)
            it.data(DataInviser)
            skilerHit.add(it)
        }
        mover.slotHide.add { it.has<DataInviser>() }
    }

    private object DataInviser : Data
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
        solider.add(tls.neut, TpSolid.builder, false) {
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
        solider.add(tls.neut, TpSolid.builder, false) {
            solider.addTls(it, tls)
            builder.add(it, { it.further() }, { lifer.heal(it, 2) })
        }
    }
}

/*
class Electric(r: Resource, solider: Solider) {
    val tlsAkt = r.tlsAkt("electric")
    val hintTrace = r.hintTileTouch
    val tileTrace = r.tile("electric.akt")

    init {
        solider.add(KindElectric)
    }

    private object KindElectric : Kind()
    //    override fun focus() = grid().map{it.key to it.value.side}.toList()
    //
    //
    //    override fun raise(aim: Aim, pg: Pg, pgSrc: Pg, side: Side,r:Raise) {
    //        return
    //    }

    //    fun wave(pgs: HashMap<Pg, List<Voin>>,que:ArrayList<Pg>) {
    //        que.firstOrNull()?.let { pg ->
    //            que.remove(0)
    //            pgs[pg] = g.info(MsgVoin(pg)).all
    //            que.addAll(pg.near.filter { it !in pgs && g.info(MsgVoin(it)).all.isNotEmpty() })
    //            wave(pgs,que)
    //        }
    //    }
    //
    //    fun hitElectro(pgAim:Pg,pgFrom:Pg){
    //        val pgs = LinkedHashMap<Pg, List<Voin>>()
    //        pgs[pgFrom] = emptyList()
    //        val que = ArrayList<Pg>()
    //        que.add(pgAim)
    //        wave(pgs,que)
    //        pgs.remove(pgFrom)
    //        pgs.forEach{ p -> p.value.forEach{ g.make(EfkDmg(p.key,it)) }}
    //        g.traces.add(TraceElectric(pgs.map { it.key }))
    //    }
    inner class TraceElectric(val pgs: List<Pg>) : Trace() {
        override fun dabsOnGrid() =
                pgs.map { DabOnGrid(it, DabTile(tileTrace, hintTrace)) }

    }
}

class Imitator(val spoter: Spoter, solider: Solider, val objs: () -> Objs) {
    init {
        solider.add(KindImitator)
        spoter.listSkilByCopy.add { obj -> if (obj.kind == KindImitator) obj.shape.head.near.flatMap { objs().byPg(it) } else emptyList() }

    }

    private object KindImitator : Kind()
    //fun sideSpot(pg: Pg) = grid()[pg]?.side

    //    fun spotByCopy(pgSpot: Pg): List<Pg> {
    //        return pgSpot.near
    //    }
}
*/
class Builder(r: Resource) {
    val tlsAkt = TlsAkt(r.tile("build", Resource.effectAkt), r.tile("build", Resource.effectAktOff))
    val hintText = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
    val price = 5
    val fabriks = ArrayList<Fabrik>()
    val lifer: Lifer by inject()
    val spoter: Spoter by inject()
    val mover: Mover by inject()
    val objs: () -> Objs by inject()

    fun plusGold(side: Side, value: Int) {
        objs().bySide(side).by<SkilBuild, Obj>().forEach { lifer.heal(it.first, value) }
    }

    fun add(obj: Obj, zone: (Obj) -> List<Pg>, refine: (Obj) -> Unit) {
        obj.data(SkilBuild(zone, fabriks))
        lifer.change(obj, 50)
    }

    fun addFabrik(prior: Int, tile: Tile, create: (Obj) -> Unit) {
        fabriks.add(Fabrik(prior, tile, create))
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

