package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.server.Side
import java.util.*

class Voiner(val r: Resource,
             val hider: Hider,
             val drawerVoin: DrawerVoin,
             val shaper: Shaper,
             val sider: Sider,
             val lifer: Lifer,
             val enforcer: Enforcer,
             val spoter:Spoter,
             val pointControl: PointControl,
             val builder: Builder,
             val skilerMove: SkilerMove
) {
    private val flip = "flip"

    val kinds = ArrayList<Kind>()

    init{
        shaper.refinesEditor.add{obj,pg,side ->
            if(obj.kind in kinds) {
                obj[flip] = pg.x > pg.pgser.xr / 2
                sider.change(obj, side)
                spoter.refresh(obj)
            }
        }
        shaper.slotMoveAfter.add{ shapeFrom,move ->
            if(move.obj.kind in kinds) {
                val d = shapeFrom.head.x - move.shapeTo.head.x
                if(d!=0) move.obj[flip] = d > 0
            }
        }
    }

    fun add(kind: Kind,isFabric:Boolean = true,hasMove:Boolean = true) {
        kinds.add(kind)
        val tlsVoin = r.tlsVoin(kind.name)
        drawerVoin.tlsVoins[kind] = tlsVoin
        shaper.addToEditor(kind,ZetOrder.voin, tlsVoin.neut)
        lifer.kinds.add(kind)
        sider.kinds.add(kind)
        enforcer.kinds.add(kind)
        if(hasMove) skilerMove.kinds.add(kind)
        pointControl.kindsCanCapture.add(kind)
        if(isFabric) builder.addFabric(kind,tlsVoin.neut,kind.hashCode())
    }
}

class Builder(r: Resource,val lifer: Lifer,val sider: Sider, val spoter: Spoter,val shaper:Shaper,val objs:()->Objs):Skil{
    private val kindsBuild = HashMap<Kind,Pair<(Obj)-> List<Pg>,(Obj)->Unit>>()
    private val kindsFabrik = ArrayList<Kind>()
    val tilesFabrik = ArrayList<Tile>()
    val tlsAkt = TlsAkt(r.tile("build",Resource.effectAkt),r.tile("build",Resource.effectAktOff))
    val hintText = r.hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
    val price = 5

    init{
        spoter.listSkil.add{obj -> if(obj.kind in kindsBuild) this else null}
    }

    override fun akts(sideVid: Side, obj: Obj): List<Akt> {
        val dabs = tilesFabrik.map{listOf(DabTile(it),DabText(price.toString(),hintText))}
        return kindsBuild[obj.kind].first(obj).filter{shaper.canCreate(Singl(ZetOrder.voin,it))}.
                map{pg -> AktOpt(pg,tlsAkt,dabs){
            val objCreated = shaper.create(kindsFabrik[it],Singl(ZetOrder.voin,pg))
            kindsBuild[obj.kind].second(objCreated)
            lifer.damage(obj,price)
        }}
    }

    fun plusGold(side:Side,value:Int){
        sider.objsSide(side).byKind(kindsBuild.keySet()).forEach { lifer.heal(it,value) }
    }

    fun add(kind:Kind,zone:(Obj)-> List<Pg>,refine:(Obj)->Unit){
        kindsBuild[kind] = zone to refine
    }

    fun addFabric(kind:Kind,tile:Tile,prior:Int){
        kindsFabrik.add(kind)
        tilesFabrik.add(tile)
    }
}

class Telepath(r: Resource, val enforcer: Enforcer, val voiner: Voiner, val spoter: Spoter):Skil {
    val tlsAkt = r.tlsAkt("telepath")

    init {
        voiner.add(KindTelepath)
        spoter.listSkil.add{ if(it.kind == KindTelepath) this else null }
    }

    override fun akts(sideVid: Side, obj: Obj) =
            obj.shape.head.near.filter { enforcer.canEnforce(it) }.map {
                AktSimple(it, tlsAkt) {
                    enforcer.enforce(it)
                    spoter.tire(obj)
                }
            }

    private object KindTelepath : Kind()
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

class Electric(r: Resource, voiner: Voiner) {
    val tlsAkt = r.tlsAkt("electric")
    val hintTrace = r.hintTileTouch
    val tileTrace = r.tile("electric.akt")

    init {
        voiner.add(KindElectric)
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

class Inviser(voiner: Voiner, val hider: Hider, val sider: Sider, val stager: Stager, val objs: () -> Objs) {
    init {
        voiner.add(KindInviser)
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

class Imitator(val spoter: Spoter, voiner: Voiner, val objs: () -> Objs) {
    init {
        voiner.add(KindImitator)
        spoter.listSkilByCopy.add{obj -> if(obj.kind == KindImitator) obj.shape.head.near.flatMap { objs().byPg(it) } else emptyList()}

    }

    private object KindImitator : Kind()
    //fun sideSpot(pg: Pg) = grid()[pg]?.side

    //    fun spotByCopy(pgSpot: Pg): List<Pg> {
    //        return pgSpot.near
    //    }
}

class Redeployer(voiner: Voiner,builder:Builder) {
    init {
        voiner.add(KindRedeployer,false,false)
        builder.add(KindRedeployer,{it.near()},{})

    }

    private object KindRedeployer : Kind()
}

class Warehouse(voiner: Voiner,builder:Builder,val lifer: Lifer) {
    init {
        voiner.add(KindWarehouse,false)
        builder.add(KindWarehouse,{it.further()},{lifer.heal(it,1)})
    }

    private object KindWarehouse : Kind()
}

class Staziser(r: Resource, val stazis: Stazis, voiner: Voiner, val spoter: Spoter) : Skil {
    val tlsAkt = r.tlsAkt("staziser")
    val tlsMove = r.tlsAktMove

    init {
        voiner.add(KindStaziser)
        spoter.listSkil.add{ if(it.kind==KindStaziser) this else null }
    }

    override fun akts(sideVid: Side, obj: Obj) =
            obj.shape.head.near.filter { it !in stazis }.map {
                AktSimple(it, tlsAkt) {
                    stazis.plant(it)
                    spoter.tire(obj)
                }
            }

    private object KindStaziser : Kind()
    //    fun spot(pgSpot: Pg,pgSrc: Pg, sideVid: Side, s: Spot) {
    //        grid()[pgSrc]?.let { voin ->
    //            for (pgNear in pgSpot.near) {
    //                if(arm.canSkil(pgSpot, pgNear, sideVid)){
    //                    s.add(pgNear, tlsAkt) {
    //                        stazis.plant(pgNear)
    //                        voin.energy = 0
    //                    }
    //                }
    //            }
    //            for (pgNear in pgSpot.near) {
    //                val reveal = arm.canMove(Move(pgSpot, pgNear, ZetOrder.unit, false, sideVid))
    //                if (reveal != null) {
    //                    s.add(pgNear, tlsMove) {
    //                        if (reveal()) {
    //                            move(voin,pgSpot,pgNear)
    //                            voin.energy -= 1
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //    }

    //    private fun move(voin:VoinSimple,pgFrom:Pg,pgTo:Pg){
    //        grid().move(pgFrom, pgTo)
    //        val xd = pgFrom.x - pgTo.x
    //        if (xd != 0) voin.flip = xd > 0
    //    }
}