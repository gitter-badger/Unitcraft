package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.lander.ObjLand
import unitcraft.lander.TpObj
import unitcraft.server.Err
import unitcraft.server.Side
import unitcraft.server.lzy
import java.util.*

class Objer(r: Resource) {
    private val hintTileFlipHide = r.hintTile("ctx.translate(rTile,0);ctx.scale(-1,1);ctx.globalAlpha=0.7;")
    private val hintTileFlip = r.hintTile("ctx.translate(rTile,0);ctx.scale(-1,1);")
    private val hintTileHide = r.hintTile("ctx.globalAlpha=0.7;")
    private val hintTextEnergy = r.hintText("ctx.fillStyle = 'lightblue';ctx.translate(0.3*rTile,0);")

    private val tilesEditor = ArrayList<Tile>()
    private val creates = ArrayList<(Obj) -> Unit>()

    private val landTps = HashMap<TpObj, MutableList<(Obj) -> Unit>>()

    val drawer: Drawer by inject()
    val mover: Mover  by inject()
    val builder: Builder by inject()
    val spoter: Spoter by inject()
    val stager: Stager by inject()
    val objs: () -> Objs by injectObjs()
    val allData: () -> AllData by injectAllData()

    val slotDrawObjPre = r.slot<AideDrawObj>("До рисования объекта")
    val slotDrawObjPost = r.slot<AideDrawObj>("После рисования объекта")

    init {
        injectValue<Editor>().onEdit(PriorDraw.obj, tilesEditor, { pg, side, num ->
            objs()[pg]?.let { objs().remove(it) }
            val obj = Obj(pg)
            objs().list.add(obj)
            obj.side = side
            obj.isFresh = true
            creates[num](obj)
        }, { pg ->
            objs()[pg]?.let {
                objs().remove(it)
            } ?: false
        })
        mover.slotMoveAfter.add { move ->
            if (!move.isKick) {
                val d = move.pgFrom.x - move.pgTo.x
                if (d != 0) move.obj.flip = d > 0
            }
            false
        }

        val tileObjNull = r.tile("null.obj")
        val tileGrave = r.tile("grave")
        val htCorpse = r.hintTile("ctx.globalAlpha=0.8;ctx.translate(0.3*rTile,0.3*rTile);ctx.scale(0.4,0.4);")
        drawer.slotDraw.add(10, this, "рисует объекты") {
            fun tile(obj: Obj): Tile {
                val data = obj.orNull<DataTileObj>()
                return if (data == null) tileObjNull
                else if (obj.side == null) data.tlsObj.neut
                else if (allData().needJoin) data.tlsObj.join(obj.side == Side.a)
                else if (obj.isAlly(side)){
                    when(spoter.objState(obj)){
                        ObjState.ready -> if(stager.isTurn(side)) data.tlsObj.allyMyTurn else data.tlsObj.ally
                        ObjState.needTire -> data.tlsObj.allyNeedTire
                        ObjState.tire -> data.tlsObj.allyTire
                    }
                } else {
                    if(spoter.objState(obj)==ObjState.tire) data.tlsObj.enemyTire
                    else data.tlsObj.enemy
                }
            }
            for (obj in allData().corpses.sort()) {
                drawTile(obj.pg, tileGrave)
                drawTile(obj.pg, tile(obj), htCorpse)
            }
            for (obj in objs().sort()) if (obj.isVid(side)) {
                val aide = AideDrawObj(this, obj, side)
                slotDrawObjPre.exe(aide)
                val hidedForVs = !obj.isVid(side.vs)
                val hint = when {
                    obj.flip && hidedForVs -> hintTileFlipHide
                    obj.flip -> hintTileFlip
                    hidedForVs -> hintTileHide
                    else -> null
                }
                val tile = tile(obj)
                drawTile(obj.pg, tile, hint)
                slotDrawObjPost.exe(aide)
            }
        }
    }

    fun add(tile: Tile,
            priorFabrik: Int?,
            tpObj: TpObj? = null,
            hasMove: Boolean = true,
            create: (Obj) -> Unit
    ) {
        tilesEditor.add(tile)
        val crt = if (hasMove) { obj -> obj.add(SkilMove());create(obj) } else create
        creates.add(crt)
        if (priorFabrik != null) builder.addFabrik(priorFabrik, tile, crt)
        if (tpObj != null) landTps.getOrPut(tpObj) { ArrayList<(Obj) -> Unit>() }.add(crt)
    }

    fun editChange(pg: Pg, sideVid: Side) {
        objs()[pg]?.let {
            it.side = when (it.side) {
                null -> sideVid
                sideVid -> sideVid.vs
                sideVid.vs -> null
                else -> throw Err("unknown side=${it.side}")
            }
        }
    }

    val maxFromTpSolid by lzy{ landTps.mapValues { it.value.size }}

    fun reset(objLands: Map<Pg,ObjLand>) {
        val objs = objs()
        for ((pg,objLand) in objLands) {
            val solid = Obj(pg)
            solid.side = objLand.side
            solid.isFresh = true
            landTps[objLand.tpObj]!![objLand.num](solid)
            objs.list.add(solid)
        }
    }

    fun setTls(obj: Obj, tlsObj: TlsObj) {
        obj.add(DataTileObj(tlsObj))
    }
}

class AideDrawObj(val ctx: CtxDraw, val obj: Obj, val side: Side) : Aide

class DataTileObj(val tlsObj: TlsObj) : Data

class Telepath(r: Resource) {
    init {
        val enforcer = injectValue<Enforce>()
        val spoter = injectValue<Spoter>()
        val tls = r.tlsVoin("telepath")
        val tlsAkt = r.tileAkt("telepath")
        injectValue<Objer>().add(tls.neut, 60) {
            it.add(DataTileObj(tls))
            it.add(DataTelepath)
        }
        spoter.addSkil<DataTelepath>() { sideVid, obj ->
            obj.near().filter { enforcer.canEnforce(it, sideVid) }.map {
                AktSimple(it, tlsAkt) {
                    enforcer.enforce(it)
                    spoter.tire(obj)
                }
            }
        }
    }

    object DataTelepath : Data
}

class Staziser(r: Resource) {
    init {
        val stazis = injectValue<Stazis>()
        val tls = r.tlsVoin("staziser")
        val tlsAkt = r.tileAkt("staziser")
        injectValue<Objer>().add(tls.neut, 50) {
            it.add(DataTileObj(tls))
            it.add(DataStaziser)
        }
        val spoter = injectValue<Spoter>()
        spoter.addSkil<DataStaziser>() { sideVid, obj ->
            obj.near().filter { !stazis.hasStazis(it) }.map {
                AktSimple(it, tlsAkt) {
                    stazis.plant(it)
                    spoter.tire(obj)
                }
            }
        }
    }

    private object DataStaziser : Data
}


class Electric(r: Resource) {
    init {
        val tls = r.tlsVoin("electric")
        injectValue<Objer>().add(tls.neut, 3) {
            it.add(DataTileObj(tls))
            it.add(DataElectric)
        }
        val tileAkt = r.tileAkt("electric")
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        val tracer = injectValue<Tracer>()
        val objs = injectObjs().value
        spoter.addSkilByBuilder<DataElectric> {
            obj.near().filter { objs()[it]?.let{lifer.canDamage(it) && it.isVid(sideVid)}?:false }.forEach {
                akt(it, tileAkt) {
                    val aims = wave(it, lifer, obj.pg)
                    lifer.damagePgs(aims, 1)
                    aims.forEach { tracer.touch(it, tileAkt) }
                    spoter.tire(obj)
                }
            }
        }
    }

    private fun wave(start: Pg, lifer: Lifer, pgExclude: Pg): List<Pg> {
        val wave = ArrayList<Pg>()
        val que = ArrayList<Pg>()
        que.add(start)
        wave.add(start)
        while (true) {
            if (que.isEmpty()) break
            val next = que.removeAt(0).near.filter { lifer.canDamage(it) && it != pgExclude && it !in wave }
            que.addAll(next)
            wave.addAll(next)
        }
        return wave
    }

    private object DataElectric : Data
}


class Imitator(r: Resource) {
    init {
        val objs = injectObjs().value
        val tls = r.tlsVoin("imitator")
        val dataTileObj = DataTileObj(tls)
        injectValue<Objer>().add(tls.neut, null, null, false) {
            it.add(dataTileObj)
            it.add(DataImitator())
        }
        val tlsAkt = r.tileAkt("imitator")
        injectValue<Spoter>().addSkil<DataImitator> { sideVid, obj ->
            val data = obj<DataImitator>()
            if (data.charged)
                obj.near().map { objs()[it] }.filterNotNull().filter { !it.has<DataImitator>() }.map {
                    AktSimple(it.pg, tlsAkt) {
                        obj.datas.clear()
                        obj.datas.addAll(it.datas)
                        data.charged = false
                        obj.add(data)
                    }
                } else emptyList()
        }

        injectValue<Stager>().slotTurnEnd.add(35, this, "имитаторы преращаются обратно") {
            objs().bothBy<DataImitator>().forEach {
                it.first.datas.clear()
                it.second.charged = true
                it.first.add(it.second)
                it.first.add(dataTileObj)
            }
        }
        //        injectValue<Drawer>().onObj<DataImitator> { obj, data, side ->
        //            if(data.charged) obj.data()
        //        }
    }

    private class DataImitator() : Data {
        var charged = true
    }
}

class Tiger(r: Resource) {
    init {
        val tls = r.tlsVoin("tiger")
        injectValue<Objer>().add(tls.neut, 3) {
            it.add(DataTileObj(tls))
            it.add(DataFrog())
        }
        val mover = injectValue<Mover>()
        val tileAkt = r.tileAkt("tiger")
        val lifer = injectValue<Lifer>()
        val tracer = injectValue<Tracer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataFrog> {
            val data = obj<DataFrog>()
            if (data.drLastLeap != null) modal()
            if(data.charges>0) for (dr in Dr.values().filter { dr -> dr != data.drLastLeap }) {
                val pgAim = obj.pg.plus(dr) ?: continue
                val pgTo = pgAim.plus(dr) ?: continue
                if (mover.isMove(obj, pgAim, sideVid)) continue
                val ok = mover.move(obj, pgTo, sideVid)
                if (ok != null) akt(pgAim, tileAkt) {
                    if (ok()) {
                        data.drLastLeap = -dr
                        data.charges -= 1
                        lifer.damage(pgAim, 1)
                        tracer.touch(pgAim, tileAkt)
                    }
                }
            }
        }
        spoter.listOnTire.add { obj ->
            obj.data<DataFrog>{
                it.drLastLeap = null
                it.charges = 50
            }
        }
    }

    private class DataFrog : Data {
        var drLastLeap: Dr? = null
        var charges = 50
    }
}

class Kicker(r: Resource) {
    init {
        val objs = injectObjs().value
        val tls = r.tlsVoin("kicker")
        injectValue<Objer>().add(tls.neut, 15) {
            it.add(DataTileObj(tls))
            it.add(DataKicker)
        }

        val tileAkt = r.tileAkt("kicker")
        val lifer = injectValue<Lifer>()
        val mover = injectValue<Mover>()
        val tracer = injectValue<Tracer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataKicker> {
            for (dr in Dr.values()) {
                val aim = obj.pg.plus(dr)?.let { objs()[it] } ?: continue
                if(!aim.isVid(sideVid)) continue
                val ray = rayKick(aim, dr, sideVid, mover)
                if (ray.isNotEmpty() || lifer.canDamage(aim)) akt(aim.pg, tileAkt) {
                    mover.kickPath(aim, ray, sideVid)
                    lifer.damage(aim, 1)
                    tracer.touch(aim.pg, tileAkt)
                    spoter.tire(obj)
                }
            }
        }
    }

    private fun rayKick(obj: Obj, dr: Dr, side: Side, mover: Mover): List<Pg> {
        val ray = ArrayList<Pg>()
        var cur = obj.pg
        while (true) {
            val next = cur.plus(dr) ?: break
            if (mover.isKick(obj, cur, next, side)) {
                ray.add(next)
                cur = next
            } else break
        }
        return ray
    }

    private object DataKicker : Data
}

class Jumper(r: Resource) {
    init {
        val tls = r.tlsVoin("jumper")
        injectValue<Objer>().add(tls.neut, 40, null, false) {
            it.add(DataTileObj(tls))
            it.add(DataJumper())
        }

        val objs = injectObjs().value
        val tileAkt = r.tileAkt("jumper")
        val tileHit = r.tileAkt("jumper","hit")
        val tileChoice = r.tileAkt("jumper", "choice")
        val lifer = injectValue<Lifer>()
        val mover = injectValue<Mover>()
        val spoter = injectValue<Spoter>()
        val tracer = injectValue<Tracer>()
        spoter.addSkilByBuilder<DataJumper> {
            val data = obj<DataJumper>()
            val pgDest = data.pgDest
            fun isAim(cand:Obj) = cand != obj && !cand.pg.isNear(obj.pg) && cand.isVid(sideVid)
            fun pgsAimNear(dst:Pg) = dst.near.filter { objs()[it]?.let{isAim(it)} ?: false }
            if (pgDest == null) objs().filter { isAim(it) }.flatMap { it.near() }.distinct().forEach {
                val can = mover.move(obj, it, sideVid)
                if (can != null) {
                    val aim = pgsAimNear(it).singleOrNull()
                    if (aim != null) akt(it, tileAkt) {
                        if (can()){
                            lifer.damage(aim, 1)
                            tracer.touch(aim, tileHit)
                            spoter.tire(obj)
                        }
                    } else akt(it, tileChoice) { data.pgDest = it }
                }
            } else pgsAimNear(pgDest).forEach {
                val can = mover.move(obj, pgDest, sideVid)
                if (can != null) akt(it, tileHit) {
                    if (can()) {
                        lifer.damage(it, 1)
                        tracer.touch(it,tileHit)
                        spoter.tire(obj)
                    }
                }
            }
        }
        spoter.listOnTire.add { obj ->
            obj.orNull<DataJumper>()?.pgDest = null
        }
    }


    private class DataJumper : Data {
        var pgDest: Pg? = null
    }
}

class Pusher(r: Resource) {
    val objs by injectObjs()
    val slotStopPush = ArrayList<(Pair<Dr, List<Obj>>) -> Boolean>()

    init {
        val tls = r.tlsVoin("pusher")
        injectValue<Objer>().add(tls.neut,null) {
            it.add(DataTileObj(tls))
            it.add(DataPusher)
        }

        val tileAkt = r.tileAkt("pusher")
        val tileHit = r.tileAkt("pusher", "hit")
        val lifer = injectValue<Lifer>()
        val mover = injectValue<Mover>()
        val spoter = injectValue<Spoter>()
        val tracer = injectValue<Tracer>()
        val skilerMove = injectValue<SkilerMove>()
        spoter.addSkilByBuilder<DataPusher> {
            if (skilerMove.fuel(obj) >= 1) for (dr in Dr.values()) {
                val pgDr = obj.pg.plus(dr) ?: continue
                val aims = objsLine(obj, dr)
                if (aims.isNotEmpty()) {
                    if (!slotStopPush.any { it(dr to aims) } && mover.isMovePhantom(obj, obj.pg, pgDr, sideVid)) akt(pgDr, tileAkt) {
                        val last = aims.last()
                        if (last.pg.isEdge()) {
                            aims.removeAt(aims.lastIndex)
                            mover.remove(last)
                        }
                        mover.jumpAll(aims.map { it to it.pg.plus(dr)!! })
                        mover.move(obj, obj.pg.plus(dr)!!, sideVid)?.invoke()
                        aims.forEach { tracer.touch(it.pg, tileAkt) }
                        skilerMove.spend(obj)
                        spoter.tire(obj)
                    }
                }/* else for (pg in pgDr.ray(dr, skilerMove.fuel(obj))) {
                    val aim = objs()[pg]
                    if (aim != null && lifer.canDamage(aim)) {
                        akt(pgDr, tileHit) {

                            lifer.damage(aim, obj.pg.distance(aim.pg) - 1)
                            spoter.tire(obj)
                        }
                        break
                    }
                }*/
            }
        }
    }

    private fun objsLine(obj: Obj, dr: Dr): ArrayList<Obj> {
        val list = ArrayList<Obj>()
        var objNext = obj
        while (true) {
            objNext = objNext.pg.plus(dr)?.let { objs()[it] } ?: break
            list.add(objNext)
        }
        return list
    }

    private object DataPusher : Data
}

class Spider(r: Resource) {
    init {
        val adhesive = injectValue<Adhesive>()
        val tls = r.tlsVoin("spider")
        val tlsAkt = r.tileAkt("spider")
        injectValue<Objer>().add(tls.neut, 50) {
            it.add(DataTileObj(tls))
            it.add(DataSpider)
        }
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataSpider> {
            obj.near().filter { adhesive.canAdhesive(it) }.forEach {
                akt(it, tlsAkt) {
                    adhesive.plant(it)
                    lifer.poison(obj, 1)
                    spoter.tire(obj)
                }
            }
        }

        adhesive.slop.add(this,"паукам разрешено"){ obj.has<DataSpider>() }
    }

    private object DataSpider : Data
}

