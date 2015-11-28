package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpFlat
import unitcraft.server.Err
import unitcraft.server.Side
import java.util.*
import kotlin.properties.Delegates

class Flater(r: Resource) {
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
            val flat = Flat(pg)
            creates[num](flat, side)
            flats().add(flat)
        }, { pg -> false })

        stager.onEndTurn {
            for ((flat, point) in allData().flats.by<DataPoint, Flat>()) {
                val obj = allData().objs[flat.pg]
                if (obj != null && !obj.side.isN)
                    point.side = obj.side
            }
        }
        val grounds = r.grounds
        injectValue<Drawer>().onFlat<DataPoint> { flat, data, side ->
            (if (stager.isBeforeTurn(side)) grounds[data.side.ordinal + 2] else if (data.side == side) grounds[0] else grounds[1]) to data.tile
        }
    }

    fun add(tileEditor: Tile, tpFlat: TpFlat, create: (Flat) -> Unit) {
        addPoint(tileEditor, tpFlat, { f, s -> create(f) })
    }

    fun addPoint(tileEditor: Tile, tpFlat: TpFlat?, create: (Flat, Side) -> Unit) {
        tilesEditor.add(tileEditor)
        creates.add(create)
        if(tpFlat!=null) landTps.getOrPut(tpFlat) { ArrayList<(Flat, Side) -> Unit>() }.add(create)
    }

    fun maxFromTpFlat() = landTps.mapValues { it.value.size }

    fun reset(flatsL: ArrayList<unitcraft.land.Flat>) {
        val flats = allData().flats
        for (flatL in flatsL) {
            val flat = Flat(flatL.pg)
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
        mover.slotHide.add { flats()[it.pg].has<DataForest>() }
        mover.slotMoveAfter.add { move ->
            if (flats()[move.pgFrom].has<DataForest>()) mover.revealUnhided()
            false
        }
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
        val skil = createSkil {
            obj.pg.all.map { pg -> mover.canMove(obj, pg, sideVid){ spoter.tire(obj) }?.let{ akt(pg, tileAkt){it()} } }
        }
        spoter.listSkil.add {
            if (it.pg in flats().by<Catapult, Flat>().map { it.first.pg }) skil else null
        }
    }

    object Catapult : Data
}

/** если юнит стоит на крепости, то его нельзя поранить*/
class Fortress(r: Resource) {
    init {
        val tile = r.tile("fortress")
        val ground = r.tileGround
        injectValue<Drawer>().onFlat<Fortress> { flat, data, side -> ground to tile }
        injectValue<Flater>().add(tile, TpFlat.special) { it.data(Fortress) }
        val flats = injectFlats().value
        injectValue<Lifer>().slotStopDamage.add{ obj -> flats()[obj.pg].has<Fortress>()}
    }

    object Fortress : Data
}

abstract class DataPoint(val tile: Tile, side: Side) : Data {
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
    val flats by injectFlats()
    init {
        val allData = injectAllData().value
        val tile = r.tile("flag")
        injectValue<Flater>().addPoint(tile, TpFlat.flag) { flat, side ->
            flat.data(DataFlag(tile, side))
        }


        injectValue<Stager>().onEndTurn { side ->
            val sideLost = sideMost().vs
            allData().point[sideLost] = allData().point[sideLost]!! - 1
        }
    }

    fun sideMost():Side{
        val flags = flats().by<DataFlag, Flat>()
        return if(flags.filter { it.second.side == Side.a }.size*2>flags.size) Side.a else Side.b
    }

    private class DataFlag(tile: Tile, side: Side) : DataPoint(tile, side)
}

class Mine(r: Resource) {
    init {
        val builder = injectValue<Builder>()
        val flats = injectFlats().value
        val tile = r.tile("mine")
        injectValue<Flater>().addPoint(tile, TpFlat.special) { flat, side -> flat.data(Mine(tile, side)) }
        injectValue<Stager>().onEndTurn { side ->
            val gold = flats().by<Mine, Flat>().filter { it.second.side == side }.size
            builder.plusGold(side, gold)
        }
    }

    private class Mine(tile: Tile, side: Side) : DataPoint(tile, side)
}

class Hospital(r: Resource) {
    init {
        val tile = r.tile("hospital")
        injectValue<Flater>().addPoint(tile, null) { flat, side -> flat.data(Hospital(tile, side)) }
    }

    private class Hospital(tile: Tile, side: Side) : DataPoint(tile, side)
}