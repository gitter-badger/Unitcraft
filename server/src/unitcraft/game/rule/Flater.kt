package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpFlat
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*
import kotlin.properties.Delegates

class Flater(r:Resource) {
    val allData: () -> AllData by injectAllData()
    val editor: Editor by inject()

    val tilesEditor = ArrayList<Tile>()
    val creates = ArrayList<(Flat, Side) -> Unit>()

    private val landTps = HashMap<TpFlat, MutableList<(Flat, Side) -> Unit>>()

    fun flats() = allData().flats

    init {
        val stager = injectValue<Stager>()
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
        val grounds = r.grounds
        injectValue<Drawer>().onFlat<DataPoint> { flat, data, side  ->
            (if (stager.isBeforeTurn(side)) grounds[data.side.ordinal + 2] else if (data.side == side) grounds[0] else grounds[1]) to data.tile
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
    injectValue<Drawer>().onFlat<D> { flat, data, side -> tiles[flat.hashCode() % tiles.size] to null }
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
class Catapult(r: Resource) {
    init {
        val tile = r.tile("catapult")
        val ground = r.tileGround
        injectValue<Drawer>().onFlat<Catapult> { flat, data, side -> ground to tile }
        injectValue<Flater>().add(tile, TpFlat.special) { it.data(Catapult) }

        val spoter = injectValue<Spoter>()
        val mover = injectValue<Mover>()
        val tileAkt = r.tileAkt("catapult")
        val flats = injectFlats().value
        val skil = { side: Side, obj: Obj, objSrc: Obj ->
            obj.shape.head.all.map { pg ->
                val move = Move(obj, obj.shape.headTo(pg), side)
                val can = mover.canMove(move)
                if (can != null) AktSimple(pg, tileAkt) {
                    if (can()) {
                        mover.move(move)
                        spoter.tire(obj)
                    }
                } else null
            }.filterNotNull()
        }
        spoter.listSkil.add {
            if (it.shape.pgs.intersect(flats().by<Catapult, Flat>().flatMap { it.first.shape.pgs }).isNotEmpty()) skil else null
        }
    }

    object Catapult : Data
}

abstract class DataPoint(val tile:Tile,side: Side) : Data {
    var side by Delegates.vetoable(side.apply { if (this == Side.n) throw Err("Side.n is not allowed") }) { prop, sideOld, sideNew -> sideNew != Side.n }
}

//private inline fun <reified D : DataPoint> groundTilePoint(tile: Tile, grounds: List<Tile>) {
//    val stager = injectValue<Stager>()
//    injectValue<Drawer>().onFlat<D> { flat, data, side ->
//        val flag = flat<D>()
//        (if (stager.isBeforeTurn(side)) grounds[flag.side.ordinal + 2] else if (flag.side == side) grounds[0] else grounds[1]) to tile
//    }
//}

class Flag(r: Resource) {
    init {
        val allData = injectAllData().value
        val tile = r.tile("flag")
        injectValue<Flater>().addPoint(tile, TpFlat.flag) { flat, side ->
            flat.data(DataFlag(tile,side))
        }

        val flats = injectFlats().value
        injectValue<Stager>().onEndTurn { side ->
            val p = flats().by<DataFlag,Flat>().partition { it.second.side==side}
            if(p.first.size<=p.second.size)
                allData().point[side] = allData().point[side]!! - 1
            if(p.first.size>=p.second.size)
                allData().point[side.vs] = allData().point[side.vs]!! - 1
        }
    }

    private class DataFlag(tile:Tile,side:Side):DataPoint(tile,side)
}

class Mine(r: Resource) {
    init {
        val builder = injectValue<Builder>()
        val flats = injectFlats().value
        val tile = r.tile("mine")
        injectValue<Flater>().addPoint(tile, TpFlat.special) { flat, side -> flat.data(Mine(tile,side)) }
        injectValue<Stager>().onEndTurn {side ->
            val gold = flats().by<Mine, Flat>().filter { it.second.side == side }.size
            builder.plusGold(side,gold)
        }
    }

    private class Mine(tile:Tile,side: Side) : DataPoint(tile,side)
}

class Hospital(r: Resource) {
    init {
        val tile = r.tile("hospital")
        injectValue<Flater>().addPoint(tile, TpFlat.special) { flat, side -> flat.data(Hospital(tile,side)) }
    }

    private class Hospital(tile:Tile,side: Side) : DataPoint(tile,side)
}