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
    val skilerMove: SkilerMove by inject()
    val objs: () -> Objs by injectObjs()

    init {
        injectValue<Editor>().onEdit(PriorDraw.obj, tilesEditor, { pg, side, num ->
            objs()[pg]?.let { objs().remove(it) }
            val obj = Obj(pg)
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
            if (!obj.isVid(side)) ctx.drawTile(obj.pg, tileHide)
        }
        mover.slotMoveAfter.add { move ->
            val d = move.pgFrom.x - move.pgTo.x
            if (d != 0) move.obj.flip = d > 0
            false
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
        val crt = if (hasMove) { obj -> create(obj); obj.data(SkilMove()) } else create
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
            val solid = Obj(solidL.pg)
            solid.side = solidL.side
            solid.isFresh = true
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
        val spoter = injectValue<Spoter>()
        val tls = r.tlsVoin("telepath")
        val tlsAkt = r.tileAkt("telepath")
        injectValue<Solider>().add(tls.neut,10) {
            it.data(DataTileObj(tls))
            it.data(DataTelepath)
        }
        spoter.addSkil<DataTelepath>(){sideVid, obj ->
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
        injectValue<Solider>().add(tls.neut,5) {
            it.data(DataTileObj(tls))
            it.data(DataStaziser)
        }
        val spoter = injectValue<Spoter>()
        spoter.addSkil<DataStaziser>(){sideVid, obj ->
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

class Inviser(r: Resource) {
    init {
        val mover = injectValue<Mover>()
        val tls = r.tlsVoin("inviser")
        injectValue<Solider>().add(tls.neut,30) {
            it.data(DataTileObj(tls))
            it.data(DataInviser)
            it.data(DataHit(-1))
        }
        mover.slotHide.add { it.has<DataInviser>() }
    }

    private object DataInviser : Data
}

class Electric(r: Resource) {
    init {
        val tls = r.tlsVoin("electric")
        injectValue<Solider>().add(tls.neut,3){
            it.data(DataTileObj(tls))
            it.data(DataElectric)
        }

        val tlsAkt = r.tileAkt("electric")
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkil<DataElectric>(){ sideVid, obj ->
            obj.near().filter { lifer.canDamage(it) }.map {
                AktSimple(it, tlsAkt) {
                    wave(it,lifer,obj.pg).forEach { lifer.damage(it,1) }
                    spoter.tire(obj)
                }
            }
        }
    }

    private fun wave(start: Pg, lifer: Lifer, pgExclude:Pg): List<Pg> {
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
        injectValue<Solider>().add(tls.neut,null,null,false) {
            it.data(dataTileObj)
            it.data(DataImitator())
        }
        val tlsAkt = r.tileAkt("imitator")
        injectValue<Spoter>().addSkil<DataImitator> { sideVid, obj ->
            val data = obj<DataImitator>()
            if(data.charged)
            obj.near().map{objs()[it]}.filterNotNull().filter{!it.has<DataImitator>()}.map{ AktSimple(it.pg, tlsAkt) {
                obj.datas.clear()
                obj.datas.addAll(it.datas)
                data.charged = false
                obj.data(data)
            }} else emptyList()
        }

        injectValue<Stager>().onEndTurn { objs().by<DataImitator,Obj>().forEach {
            it.first.datas.clear()
            it.second.charged = true
            it.first.data(it.second)
            it.first.data(dataTileObj)
        }}
//        injectValue<Drawer>().onObj<DataImitator> { obj, data, side ->
//            if(data.charged) obj.data()
//        }
    }

    private class DataImitator() : Data{
        var charged = true
    }
}

class Frog(r:Resource){
    init {
        val tls = r.tlsVoin("frog")
        injectValue<Solider>().add(tls.neut,3){
            it.data(DataTileObj(tls))
            it.data(DataFrog())
        }
        val mover = injectValue<Mover>()
        val tlsAkt = r.tileAkt("frog")
        val lifer = injectValue<Lifer>()
        val spoter = injectValue<Spoter>()
        spoter.addSkilByBuilder<DataFrog> {
            val data = obj<DataFrog>()
            if(data.drLastLeap!=null) modal()
            Dr.values.filter{ dr ->
                dr!=data.drLastLeap && obj.pg.plus(dr)?.let{
                    mover.canMove(Move(obj,it,sideVid))==null
                }?:false
            }.forEach { dr ->
                val move = obj.pg.plus(dr)!!.plus(dr)?.let{Move(obj, it, sideVid)}
                if(move!=null) {
                    val can = mover.canMove(move)
                    val pgAim = obj.pg.plus(dr)!!
                    if (can != null) akt(pgAim, tlsAkt) {
                        if(can()) {
                            data.drLastLeap = -dr
                            mover.move(move)
                            lifer.damage(pgAim, 1)
                        }
                    }
                }
            }
        }
        spoter.listOnTire.add{obj ->
            if(obj.has<DataFrog>()) obj<DataFrog>().drLastLeap = null
        }
    }

    private class DataFrog : Data{
        var drLastLeap:Dr? = null
    }
}

