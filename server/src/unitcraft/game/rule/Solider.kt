package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import java.util.ArrayList

class Solider(val r: Resource,
              val drawer: Drawer,
              val editor: Editor,
              val sider: Sider,
              val lifer: Lifer,
              val enforcer: Enforcer,
              val spoter: Spoter,
              val mover: Mover,
              val builder: Builder,
              val skilerMove: SkilerMove,
              objs: () -> Objs
) {
    private val hintTileFlip = r.hintTileFlip
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")
    private val tileHide = r.tile("hide")
    val tilesEditor = ArrayList<Tile>()
    val creates = ArrayList<(Obj) -> Unit>()

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
            if (obj.has<DataTlsSolid>()) {
                ctx.drawTile(obj.head(), obj<DataTlsSolid>().tlsSolid()(side, obj.side, obj.isFresh), if (obj.flip) hintTileFlip else null)
            }
            if (mover.isHided(obj, side)) ctx.drawTile(obj.head(), tileHide)
        }
        mover.slotMoveAfter.add { shapeFrom, move ->
            val d = shapeFrom.head.x - move.shapeTo.head.x
            if (d != 0) move.obj.flip = d > 0
        }
    }

    fun add(tile: Tile, isFabric: Boolean = true, hasMove: Boolean = true, create: (Obj) -> Unit) {
        tilesEditor.add(tile)
        val crt = if (hasMove) { obj -> create(obj); skilerMove.add(obj, 3) } else create
        creates.add(crt)
        if (isFabric) builder.addFabrik(0, tile, crt)
    }
}

abstract class DataTlsSolid : Data {
    abstract fun tlsSolid(): TlsSolid
}

open class DataTlsSolidFix(val tls: TlsSolid) : DataTlsSolid() {
    override fun tlsSolid() = tls
}

class Telepath(r: Resource, val enforcer: Enforcer, val solider: Solider, val spoter: Spoter) {
    init {
        val tls = r.tlsVoin("telepath")
        val tlsAkt = r.tlsAkt("telepath")
        val kind = Telepath(tls)
        val skil = SkilTelepath(enforcer, spoter, tlsAkt)
        solider.add(tls.neut) {
            it.data(kind)
            it.data(skil)
        }
    }

    private class Telepath(tlsSolid: TlsSolid) : DataTlsSolidFix(tlsSolid)

    class SkilTelepath(val enforcer: Enforcer, val spoter: Spoter, val tlsAkt: TlsAkt) : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.shape.head.near.filter { enforcer.canEnforce(it) }.map {
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

class Staziser(r: Resource, val stazis: Stazis, solider: Solider, val spoter: Spoter) {
    init {
        val tls = r.tlsVoin("staziser")
        val tlsAkt = r.tlsAkt("staziser")
        val staziser = Staziser(tls)
        solider.add(tls.neut) {
            it.data(staziser)
            it.data(SkilStaziser(tlsAkt))
        }
    }

    private class Staziser(tlsSolid: TlsSolid) : DataTlsSolidFix(tlsSolid)

    inner class SkilStaziser(val tlsAkt: TlsAkt) : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.shape.head.near.filter { it !in stazis }.map {
                    AktSimple(it, tlsAkt) {
                        stazis.plant(it)
                        spoter.tire(obj)
                    }
                }
    }
}

class Redeployer(r: Resource, solider: Solider, val builder: Builder, val spoter: Spoter, val objs: () -> Objs) {
    val tlsAkt = r.tlsAkt("redeployer")

    init {
        val tls = r.tlsVoin("redeployer")
        val dataTls = Redeployer(tls)
        val skil = SkilRedeployer()
        solider.add(tls.neut,false) {
            it.data(dataTls)
            it.data(skil)
            builder.add(it, { it.near() }, {})
        }

    }

    private class Redeployer(tlsSolid: TlsSolid) : DataTlsSolidFix(tlsSolid)

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

class Inviser(solider: Solider, val hider: Hider, val sider: Sider, val stager: Stager, val objs: () -> Objs) {
    init {
        solider.add(KindInviser)
        stager.onEndTurn { side ->
            for (obj in objs().byKind(KindInviser)) {
                if (sider.isEnemy(obj, side)) hider.hide(obj, this)
            }
        }
    }

    private object KindInviser : Kind()
    //    val hide : MutableSet<VoinStd> = Collections.newSetFromMap(WeakHashMap<VoinStd,Boolean>())

    //    make<EfkUnhide>(0) {
    //        //voins[pg]?.let{hide.remove(it)}
    //    }
    //
    //    info<MsgIsHided>(0){
    //        if(voin in hide) yes()
    //    }
    //
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

class Warehouse(solider: Solider, builder: Builder, val lifer: Lifer) {
    init {
        solider.add(KindWarehouse, false)
        builder.add(KindWarehouse, { it.further() }, { lifer.heal(it, 1) })
    }

    private object KindWarehouse : Kind()
}
*/
class Builder(r: Resource, val lifer: Lifer, val spoter: Spoter, val mover: Mover, val objs: () -> Objs) {
    //    private val kindsBuild = HashMap<Kind, Pair<(Obj) -> List<Pg>, (Obj) -> Unit>>()
    //    private val kindsFabrik = ArrayList<Kind>()
    //    val tilesFabrik = ArrayList<Tile>()
    val tlsAkt = TlsAkt(r.tile("build", Resource.effectAkt), r.tile("build", Resource.effectAktOff))
    val hintText = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
    val price = 5
    val fabriks = ArrayList<Fabrik>()

    fun plusGold(side: Side, value: Int) {
        objs().objsSide(side).by<SkilBuild, Obj>().forEach { lifer.heal(it.first, value) }
    }

    fun add(obj: Obj, zone: (Obj) -> List<Pg>, refine: (Obj) -> Unit) {
        obj.data(SkilBuild(zone, fabriks))
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

