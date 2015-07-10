package unitcraft.game.rule

import unitcraft.game.*

class Voiner(val r: Resource,
             val hider: Hider,
             val drawerVoin: DrawerVoin,
             val editorVoin: EditorVoin,
             val sider:Sider,
             val lifer:Lifer,
             val enforcer:Enforcer
) {


    fun add(kind:Kind) {
        val tlsVoin = r.tlsVoin(kind.name)
        drawerVoin.tlsVoins[kind]=tlsVoin
        editorVoin.addKind(kind,tlsVoin.neut)
        lifer.kinds.add(kind)
        sider.kinds.add(kind)
        enforcer.kinds.add(kind)
    }
}

class Telepath(r: Resource, val enforcer: Enforcer,val voiner:Voiner) {
    val tlsAkt = r.tlsAkt("telepath")

    init {
        voiner.add(KindTelepath)
    }
    private object KindTelepath:Kind()
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

class Electric(r: Resource,voiner:Voiner,val spoter: Spoter){
    val tlsAkt = r.tlsAkt("electric")
    val hintTrace = r.hintTileTouch
    val tileTrace = r.tile("electric.akt")

    init{
        voiner.add(KindElectric)
        spoter.skils[KindElectric] = listOf(Sk(tlsAkt))
    }
    private object KindElectric: Kind()
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
    inner class TraceElectric(val pgs:List<Pg>): Trace(){
        override fun dabsOnGrid() =
                pgs.map { DabOnGrid(it, DabTile(tileTrace, hintTrace)) }

    }
    class Sk(val tlsAkt: TlsAkt): Skil {
        override fun tlsAkt()=tlsAkt
    }
}

class Inviser(voiner:Voiner, val hider: Hider,val sider: Sider,val stager: Stager,val objs: () -> Objs) {
    init {
        voiner.add(KindInviser)
        stager.onEndTurn { side ->
            for (obj in objs().byKind(KindInviser)) {
                if (sider.isEnemy(obj,side)) hider.hide(obj,this)
            }
        }
    }
    private object KindInviser:Kind()
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

class Imitator(val spoter: Spoter,voiner:Voiner) {
    init{
        voiner.add(KindImitator)
    }
    private object KindImitator:Kind()
    //fun sideSpot(pg: Pg) = grid()[pg]?.side

    //    fun spotByCopy(pgSpot: Pg): List<Pg> {
    //        return pgSpot.near
    //    }
}

class Redeployer(voiner:Voiner){
    init {
        voiner.add(KindRededloyer)
    }
    private object KindRededloyer:Kind()
}

class Staziser(r: Resource,val stazis:Stazis,voiner:Voiner){
    val tlsAkt = r.tlsAkt("staziser")
    val tlsMove = r.tlsAktMove

    init{
        voiner.add(KindStaziser)
    }
    private object KindStaziser:Kind()
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