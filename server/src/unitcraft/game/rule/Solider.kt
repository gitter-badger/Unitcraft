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


    //
    private val tilesEditor = ArrayList<Tile>()
    private val creates = ArrayList<(Obj) -> Unit>()

    private val landTps = HashMap<TpSolid, MutableList<(Obj) -> Unit>>()

    val drawer: Drawer by inject()
    val mover: Mover  by inject()
    val builder: Builder by inject()
    val stager: Stager by inject()
    val skilerMove: SkilerMove by inject()
    val objs: () -> Objs by injectObjs()

    init {
        injectValue<Editor>().onEdit(PriorDraw.obj, tilesEditor, { pg, side, num ->
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
        val tileHide = r.tile("hide")
        drawer.drawObjs.add { obj, side, ctx ->
            if (!obj.isVid(side)) ctx.drawTile(obj.head(), tileHide)
        }
        mover.slotMoveAfter.add { shapeFrom, move ->
            val d = shapeFrom.head.x - move.shapeTo.head.x
            if (d != 0) move.obj.flip = d > 0
        }
        drawer.onObj<DataTileObj> { obj, data, side ->
            val hided = !obj.isVid(side.vs)
            val tile = if (stager.isBeforeTurn(side)) data.tlsObj.join(obj.side==Side.a) else data.tlsObj(side, obj.side, obj.isFresh)
            val hint = when {
                obj.flip && hided -> hintTileFlipHide
                obj.flip -> hintTileFlip
                hided -> hintTileHide
                else -> null
            }
            tile to hint
        }
    }

    fun add(tile: Tile,
            priorFabrik:Int?,
            tpSolid: TpSolid? = null,
            hasMove: Boolean = true,
            create: (Obj) -> Unit
    ) {
        tilesEditor.add(tile)
        val crt = if (hasMove) { obj -> create(obj); skilerMove.add(obj, 3) } else create
        creates.add(crt)
        if (priorFabrik!=null) builder.addFabrik(priorFabrik, tile, crt)
        if (tpSolid != null) landTps.getOrPut(tpSolid) { ArrayList<(Obj) -> Unit>() }.add(crt)
    }

    fun addTls(obj: Obj, tlsObj: TlsObj) {
        obj.data(DataTileObj(tlsObj))
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
            solid.side = solidL.side
            landTps[solidL.tpSolid]!![solidL.num](solid)
            objs().add(solid)
        }
    }

    /*inner class HasTlsSolid(val tlsSolid: TlsSolid) : HasTileObj {
        override fun tile(sideVid: Side, obj: Obj) = tlsSolid(sideVid, obj.side, obj.isFresh)

        override fun hint(sideVid: Side, obj: Obj): HintTile? {
            val hided = !obj.isVid(sideVid.vs)
            return when {
                obj.flip && hided -> hintTileFlipHide
                obj.flip -> hintTileFlip
                hided -> hintTileHide
                else -> null
            }
        }
    }*/
}

class DataTileObj(val tlsObj: TlsObj) : Data

class Telepath(r: Resource) {
    init {
        val enforcer = injectValue<Enforcer>()
        val solider = injectValue<Solider>()
        val spoter = injectValue<Spoter>()
        val tls = r.tlsVoin("telepath")
        val tlsAkt = r.tlsAkt("telepath")
        val skil = SkilTelepath(enforcer, spoter, tlsAkt)
        solider.add(tls.neut,10) {
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
        solider.add(tls.neut,5) {
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
        val mover = injectValue<Mover>()
        val skilerHit = injectValue<SkilerHit>()
        val tls = r.tlsVoin("inviser")
        injectValue<Solider>().add(tls.neut,30) {
            it.data(DataTileObj(tls))
            it.data(DataInviser)
            skilerHit.add(it)
        }
        mover.slotHide.add { it.has<DataInviser>() }
    }

    private object DataInviser : Data
}
/*
class Electric(r: Resource, solider: Solider) {
    //val tlsAkt = r.tlsAkt("electric")
    val hintTrace = r.hintTileTouch
    val tileTrace = r.tile("electric.akt")

    init {
        val tls = r.tlsVoin("electric")
        injectValue<Solider>().add(tls.neut,3){
            it.data(DataTileObj(tls))
            it.data(DataElectric)
        }
    }

    inner class SkilHit() : Skil {
        override fun akts(sideVid: Side, obj: Obj) =
                obj.near().filter { lifer.canDamage(it) }.map {
                    AktSimple(it, tlsAkt) {
                        lifer.damage(it,1)
                        spoter.tire(obj)
                    }
                }
    }

    private object DataElectric : Data
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


