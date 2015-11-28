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

    private val tilesEditor = ArrayList<Tile>()
    private val creates = ArrayList<(Obj) -> Unit>()

    private val landTps = HashMap<TpSolid, MutableList<(Obj) -> Unit>>()

    val drawer: Drawer by inject()
    val mover: Mover  by inject()
    val builder: Builder by inject()
    val stager: Stager by inject()
    val objs: () -> Objs by injectObjs()

    init {
        injectValue<Editor>().onEdit(PriorDraw.obj, tilesEditor, { pg, side, num ->
            objs()[pg]?.let { objs().remove(it) }
            mover.canAdd(pg, side) { obj, n ->
                obj.side = side
                obj.isFresh = true
                creates[num](obj)
            }?.invoke(0)
        }, { pg ->
            objs()[pg]?.let {
                objs().remove(it)
            } ?: false
        })
        val tileHide = r.tile("hide")
        drawer.drawObjs.add { obj, side, ctx ->
            if (!obj.isVid(side)) ctx.drawTile(obj.pg, tileHide)
        }
        mover.slotMoveAfter.add { move ->
            val d = move.pgFrom.x - move.pgTo.x
            if (d != 0) move.obj.flip = d > 0
            false
        }
        drawer.onObj<DataTileObj> { obj, data, side ->
            val hided = !obj.isVid(side.vs)
            val tile = if (stager.isBeforeTurn(side)) data.tlsObj.join(obj.side == Side.a) else {
                if (obj.side.isN) data.tlsObj.neut
                else {
                    if (stager.isTurn(side)) {
                        if (obj.side == side) data.tlsObj.ally(obj.isFresh)
                        else data.tlsObj.enemy(true)
                    } else {
                        if (obj.side == side) data.tlsObj.ally(false)
                        else data.tlsObj.enemy(obj.isFresh)
                    }
                }
            }
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
            priorFabrik: Int?,
            tpSolid: TpSolid? = null,
            hasMove: Boolean = true,
            create: (Obj) -> Unit
    ) {
        tilesEditor.add(tile)
        val crt = if (hasMove) { obj -> create(obj); obj.data(SkilMove()) } else create
        creates.add(crt)
        if (priorFabrik != null) builder.addFabrik(priorFabrik, tile, crt)
        if (tpSolid != null) landTps.getOrPut(tpSolid) { ArrayList<(Obj) -> Unit>() }.add(crt)
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
            val solid = Obj(solidL.pg)
            solid.side = solidL.side
            solid.isFresh = true
            landTps[solidL.tpSolid]!![solidL.num](solid)
            objs().list.add(solid)
        }
    }
}

class DataTileObj(val tlsObj: TlsObj) : Data

class Telepath(r: Resource) {
    init {
        val enforcer = injectValue<Enforcer>()
        val spoter = injectValue<Spoter>()
        val tls = r.tlsVoin("telepath")
        val tlsAkt = r.tileAkt("telepath")
        injectValue<Solider>().add(tls.neut, 60) {
            it.data(DataTileObj(tls))
            it.data(DataTelepath)
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
        injectValue<Solider>().add(tls.neut, 50) {
            it.data(DataTileObj(tls))
            it.data(DataStaziser)
        }
        val spoter = injectValue<Spoter>()
        spoter.addSkil<DataStaziser>() { sideVid, obj ->
            obj.near().filter { it !in stazis }.map {
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
        injectValue<Solider>().add(tls.neut, 3) {
            it.data(DataTileObj(tls))
            it.data(DataElectric)
        }

        val tileAkt = r.tileAkt("electric")
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        val tracer = injectValue<Tracer>()
        spoter.addSkilByBuilder<DataElectric> {
            obj.near().filter { lifer.canDamage(it) }.forEach {
                akt(it, tileAkt) {
                    wave(it, lifer, obj.pg).forEach {
                        lifer.damage(it, 1)
                        tracer.touch(it, tileAkt)
                    }
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
        injectValue<Solider>().add(tls.neut, null, null, false) {
            it.data(dataTileObj)
            it.data(DataImitator())
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
                        obj.data(data)
                    }
                } else emptyList()
        }

        injectValue<Stager>().onEndTurn {
            objs().by<DataImitator, Obj>().forEach {
                it.first.datas.clear()
                it.second.charged = true
                it.first.data(it.second)
                it.first.data(dataTileObj)
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

class Frog(r: Resource) {
    init {
        val tls = r.tlsVoin("frog")
        injectValue<Solider>().add(tls.neut, 3) {
            it.data(DataTileObj(tls))
            it.data(DataFrog())
        }
        val mover = injectValue<Mover>()
        val tileAkt = r.tileAkt("frog")
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataFrog> {
            val data = obj<DataFrog>()
            if (data.drLastLeap != null) modal()
            Dr.values.filter { dr ->
                dr != data.drLastLeap && obj.pg.plus(dr)?.let { !mover.isMove(obj, it, sideVid) } ?: false
            }.forEach { dr ->
                val pgAim = obj.pg.plus(dr)!!
                val pgTo = pgAim.plus(dr)
                if (pgTo != null) mover.canMove(obj, pgTo, sideVid) {
                    lifer.damage(pgAim, 1)
                }?.let {
                    akt(pgAim, tileAkt) {
                        it()
                        data.drLastLeap = -dr
                    }
                }
            }
        }
        spoter.listOnTire.add { obj ->
            if (obj.has<DataFrog>()) obj<DataFrog>().drLastLeap = null
        }
    }

    private class DataFrog : Data {
        var drLastLeap: Dr? = null
    }
}

class Kicker(r: Resource) {
    init {
        val objs = injectObjs().value
        val tls = r.tlsVoin("kicker")
        injectValue<Solider>().add(tls.neut, 15) {
            it.data(DataTileObj(tls))
            it.data(DataKicker)
        }

        val tileAkt = r.tileAkt("kicker")
        val lifer = injectValue<Lifer>()
        val mover = injectValue<Mover>()
        val tracer = injectValue<Tracer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataKicker> {
            for (dr in Dr.values) {
                val aim = obj.pg.plus(dr)?.let { objs()[it] } ?: continue
                //val pgTo = aim.pg.plus(dr) ?: continue
                //val can = mover.canMove(aim, pgTo, sideVid, true) { spoter.tire(obj) }
                val ray = rayKick(aim, dr, sideVid, mover)
                if (ray.isNotEmpty() || lifer.canDamage(aim)) akt(aim.pg, tileAkt) {
                    for (pg in ray) {
                        mover.canMove(aim, pg, sideVid,true){}?.let{it()}?:break
                    }
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
            if (mover.isMove(obj, cur, next, side, true)){
                ray.add(next)
                cur = next
            }else break
        }
        return ray
    }

    private object DataKicker : Data
}

class Jumper(r: Resource) {
    init {
        val objs = injectObjs().value
        val tls = r.tlsVoin("jumper")
        injectValue<Solider>().add(tls.neut, 40, null, false) {
            it.data(DataTileObj(tls))
            it.data(DataJumper())
        }

        val tileAkt = r.tileAkt("jumper")
        val tileAktDest = r.tileAkt("jumper", "dest")
        val lifer = injectValue<Lifer>()
        val mover = injectValue<Mover>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataJumper> {
            val data = obj<DataJumper>()
            val pgDest = data.pgDest
            if (pgDest == null) {
                objs().filter { it != obj && !it.pg.isNear(obj.pg) }.flatMap { it.near() }.distinct().forEach {
                    val can = mover.canMove(obj, it, sideVid) {
                        lifer.damage(it.near.map { objs()[it] }.filterNotNull(), 1)
                        spoter.tire(obj)
                    }
                    if (can != null) {
                        if (it.near.filter { it != obj.pg && lifer.canDamage(it) }.size == 1)
                            akt(it, tileAkt) { can() }
                        else
                            akt(it, tileAktDest) { data.pgDest = it }
                    }
                }
            } else {
                pgDest.near.filter { lifer.canDamage(it) }.forEach {
                    val can = mover.canMove(obj, pgDest, sideVid) { lifer.damage(it, 1) }
                    if (can != null) akt(it, tileAkt) {
                        can()
                        data.pgDest = null
                        spoter.tire(obj)
                    }
                }
            }
        }
    }

    private class DataJumper : Data {
        var pgDest: Pg? = null
    }
}