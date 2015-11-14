package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpFlat
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*
import kotlin.properties.Delegates

class Flater {
    val allData: () -> AllData by injectAllData()
    val stager: Stager by inject()
    val editor: Editor by inject()

    val tilesEditor = ArrayList<Tile>()
    val creates = ArrayList<(Flat, Side) -> Unit>()

    private val landTps = HashMap<TpFlat, MutableList<(Flat, Side) -> Unit>>()

    fun flats() = allData().flats

    init {
        editor.onEdit(PriorDraw.flat, tilesEditor, { pg, side, num ->
            flats().remove(flats()[pg])
            val flat = Flat(Singl(pg))
            creates[num](flat, side)
            flats().add(flat)
        }, { pg -> false })
        stager.onEndTurn {
            for ((flat, point) in allData().flats.by<DataPoint, Flat>())
                for (voin in allData().objs)
                    if (flat.shape.pgs.intersect(voin.shape.pgs).isNotEmpty())
                        point.side = voin.side
        }
    }

    fun add(tileEditor: Tile, tpFlat: TpFlat, create: (Flat) -> Unit) {
        addPoint(tileEditor, tpFlat, { f, s -> create(f) })
    }

    fun addPoint(tileEditor: Tile, tpFlat: TpFlat, create: (Flat, Side) -> Unit) {
        tilesEditor.add(tileEditor)
        creates.add(create)
        landTps.getOrPut(tpFlat) { ArrayList<(Flat, Side) -> Unit>() }.add(create)
    }

    fun maxFromTpFlat() = landTps.mapValues { it.value.size }

    fun reset(flatsL: ArrayList<unitcraft.land.Flat>) {
        val flats = allData().flats
        for (flatL in flatsL) {
            val flat = Flat(flatL.shape)
            landTps[flatL.tpFlat]!![flatL.num](flat, flatL.side)
            flats.add(flat)
        }
    }
}

//val hide : MutableSet<Any> = Collections.newSetFromMap(WeakHashMap<Any,Boolean>())
//        endTurn(5) {

//grass, mount, forest, sand, hill, water
//
//val sizeFix: Map<TpPlace, Int> = mapOf(
//        TpPlace.forest to 4,
//        TpPlace.grass to 5,
//        TpPlace.hill to 1,
//        TpPlace.mount to 1,
//        TpPlace.sand to 4,
//        TpPlace.water to 1
//)

private inline fun <reified D : Data> groundTilePlace(tiles: List<Tile>) {
    injectValue<Drawer>().onFlat<D> { flat, data, side -> if (flat.has<D>()) tiles[flat.hashCode() % tiles.size] to null else null }
}

//open class HasTileFlatFix(val tile: Tile?,val ground:Tile) : HasTileFlat {
//    override fun tile(sideVid: Side, flat: Flat) = tile
//    override fun ground(sideVid: Side, flat: Flat) = ground
//}

class Sand(r: Resource) {
    init {
        val tiles = r.tlsList(4, "sand", Resource.effectPlace)
        injectValue<Flater>().add(tiles[0], TpFlat.wild) { it.data(DataSand) }
        groundTilePlace<DataSand>(tiles)
    }

    object DataSand : Data
}

class Forest(r: Resource) {
    init {
        val flats = injectFlats().value
        val tiles = r.tlsList(4, "forest", Resource.effectPlace)
        groundTilePlace<DataForest>(tiles)
        injectValue<Flater>().add(tiles[0], TpFlat.wild) { it.data(DataForest) }

        val mover = injectValue<Mover>()
        mover.slotHide.add { flats()[it.head()].has<DataForest>() }
        mover.slotMoveAfter.add { shapeFrom, move -> mover.rehide() }
    }

    object DataForest : Data
}

class Grass(r: Resource) {
    init {
        val tiles = r.tlsList(5, "grass", Resource.effectPlace)
        injectValue<Flater>().add(tiles[0], TpFlat.none) { it.data(DataGrass) }
        groundTilePlace<DataGrass>(tiles)
    }

    object DataGrass : Data
}

class Water(r: Resource) {
    init {
        val tiles = r.tlsList(1, "water", Resource.effectPlace)
        injectValue<Flater>().add(tiles[0], TpFlat.liquid) { it.data(DataWater) }
        groundTilePlace<DataWater>(tiles)
    }

    object DataWater : Data
}

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(val r: Resource) : Skil {
    val tlsAkt = r.tlsAkt("catapult")
    val spoter: Spoter by inject()
    val mover: Mover by inject()

    init {
        val flats = injectFlats().value
        spoter.listSkil.add {
            if (it.shape.pgs.intersect(flats().by<Catapult, Flat>().flatMap { it.first.shape.pgs }).isNotEmpty()) this else null
        }

        val tile = r.tile("catapult")
        val ground = r.tileGround
        injectValue<Drawer>().onFlat<Catapult> { flat, data, side -> ground to tile }
        injectValue<Flater>().add(tile, TpFlat.special) { it.data(Catapult) }
    }

    override fun akts(sideVid: Side, obj: Obj) =
            obj.shape.head.all.map { pg ->
                val move = Move(obj, obj.shape.headTo(pg), sideVid)
                val can = mover.canMove(move)
                if (can != null) AktSimple(pg, tlsAkt) {
                    if (can()) {
                        mover.move(move)
                        spoter.tire(obj)
                    }
                } else null
            }.filterNotNull()


    private object Catapult : Data
    //        info<MsgSpot>(20) {
    //            if (pgSrc in flats) g.voin(pgSpot,side)?.let {
    //                val tggl = g.info(MsgTgglRaise(pgSpot, it))
    //                if(!tggl.isCanceled) {
    //                    val r = Raise(pgSpot, tggl.isOn)
    //                    for (pg in g.pgs) if(!g.stop(EfkMove(pgSpot, pg, it))) r.add(pg, tlsAkt, EfkMove(pgSpot, pg, it))
    //                    add(r)
    //                }
    //            }
    //        }
}

abstract class DataPoint(side: Side) : Data {
    open var side by Delegates.vetoable(side.apply { if (this == Side.n) throw Err("Side.n is not allowed") }) { prop, sideOld, sideNew -> sideNew != Side.n }
}

private inline fun <reified D : DataPoint> groundTilePoint(tile:Tile,grounds: List<Tile>) {
    val stager = injectValue<Stager>()
    injectValue<Drawer>().onFlat<D> { flat, data, side ->
        val flag = flat<D>()
        (if (stager.isBeforeTurn(side)) grounds[flag.side.ordinal + 2] else if (flag.side == side) grounds[0] else grounds[1]) to tile
    }
}

class Flag(r: Resource) {
    init {
        val tile = r.tile("flag")
        val grounds = r.grounds
        groundTilePoint<DataFlag>(tile,grounds)
        injectValue<Flater>().addPoint(tile, TpFlat.flag) { flat, side -> flat.data(DataFlag(side)) }
    }

    private class DataFlag(side: Side) : DataPoint(side)
}

class Mine(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val stager = injectValue<Stager>()
        val flats = injectFlats().value
        val tile = r.tile("mine")
        val grounds = r.grounds
        groundTilePoint<Mine>(tile,grounds)
        flater.addPoint(tile, TpFlat.special) { flat, side -> flat.data(Mine(side)) }
        stager.onEndTurn {
            val gold = flats().by<Mine, Flat>().filter { it.second.side == stager.sideTurn() }.size
            //            builder.plusGold(stager.sideTurn(),gold)
        }
    }

    private class Mine(side: Side) : DataPoint(side)
}

class Hospital(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val tile = r.tile("hospital")
        val grounds = r.grounds
        groundTilePoint<Hospital>(tile,grounds)
        flater.addPoint(tile, TpFlat.special) { flat, side -> flat.data(Hospital(side)) }
    }

    private class Hospital(side: Side) : DataPoint(side)
}