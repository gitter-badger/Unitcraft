package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.inject.inject
import unitcraft.inject.injectValue
import unitcraft.land.TpFlat
import unitcraft.server.Side
import java.util.*
import kotlin.properties.Delegates

class Flater {
    val allData: () -> AllData by injectAllData()
    val stager: Stager by inject()
    val drawer: Drawer by inject()
    val editor: Editor by inject()

    val tilesEditor = ArrayList<Tile>()
    val creates = ArrayList<(Flat) -> Unit>()

    private val landTps = HashMap<TpFlat, MutableList<(Flat) -> Unit>>()

    fun flats() = allData().flats

    init {
        editor.onEdit(PriorDraw.flat, tilesEditor, { pg, side, num ->
            flats().remove(flats()[pg])
            val flat = Flat(Singl(pg))
            creates[num](flat)
            flats().add(flat)
        }, { pg -> false })
        stager.onEndTurn {
            for ((flat, point) in allData().flats.by<DataPoint>())
                for (voin in allData().objs)
                    if (flat.shape.pgs.intersect(voin.shape.pgs).isNotEmpty())
                        point.side = voin.side
        }
    }

    fun add(tileEditor: Tile, tpFlat: TpFlat, create: (Flat) -> Unit) {
        tilesEditor.add(tileEditor)
        creates.add(create)
        landTps.getOrPut(tpFlat) { ArrayList<(Flat) -> Unit>() }.add(create)
    }

    fun maxFromTpFlat() = landTps.mapValues { it.value.size }

    fun reset(flatsL: ArrayList<unitcraft.land.Flat>) {
        val flats = allData().flats
        for (flatL in flatsL) {
            val flat = Flat(flatL.shape)
            landTps[flatL.tpFlat]!![flatL.num](flat)
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

fun randomTile(tiles: List<Tile>) = tiles[Any().hashCode() % tiles.size]

open class HasTileFlatFix(val tile: Tile) : HasTileFlat {
    override fun tile(sideVid: Side, flat: Flat) = tile
}

class Sand(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val tiles = r.tlsList(4, "sand", Resource.effectPlace)
        flater.add(tiles[0], TpFlat.wild) { it.data(DataSand(randomTile(tiles))) }
    }

    class DataSand(tile: Tile) : HasTileFlatFix(tile)
}

class Forest(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val mover = injectValue<Mover>()
        val flats = injectFlats().value
        val tiles = r.tlsList(4, "forest", Resource.effectPlace)
        flater.add(tiles[0], TpFlat.wild) { it.data(DataForest(randomTile(tiles))) }
        mover.slotHide.add { flats()[it.head()].has<DataForest>() }
        mover.slotMoveAfter.add { shapeFrom, move -> mover.rehide() }
    }

    class DataForest(tile: Tile) : HasTileFlatFix(tile)
}

class Grass(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val tiles = r.tlsList(5, "grass", Resource.effectPlace)
        flater.add(tiles[0], TpFlat.none) { it.data(DataGrass(randomTile(tiles))) }
    }

    class DataGrass(tile: Tile) : HasTileFlatFix(tile)
}

class Water(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val tiles = r.tlsList(1, "water", Resource.effectPlace)
        flater.add(tiles[0], TpFlat.liquid) { it.data(DataWater(randomTile(tiles))) }
    }

    class DataWater(tile: Tile) : HasTileFlatFix(tile)
}

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(val r: Resource) : Skil {
    val tlsAkt = r.tlsAkt("catapult")
    val spoter: Spoter by inject()
    val mover: Mover by inject()

    init {
        val flater = injectValue<Flater>()
        val flats = injectFlats().value
        val tile = r.tile("catapult")
        val catapult = Catapult(tile)
        flater.add(tile, TpFlat.special) { it.data(catapult) }

        spoter.listSkil.add {
            if (it.shape.pgs.intersect(flats().by<Catapult>().flatMap { it.first.shape.pgs }).isNotEmpty()) this else null
        }
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


    private class Catapult(tile: Tile) : HasTileFlatFix(tile)
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

abstract class DataPoint(val tls: TlsFlatOwn) : HasTileFlat {
    open var side = Side.n
    override fun tile(sideVid: Side, flat: Flat) = tls(sideVid, side)
}

class Flag(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val tls = r.tlsFlatOwn("flag")
        flater.add(tls.neut, TpFlat.flagA) { it.data(DataFlag(tls)) }
        flater.add(tls.neut, TpFlat.flagB) { it.data(DataFlag(tls)) }
    }

    private class DataFlag(tls: TlsFlatOwn) : DataPoint(tls) {
        override var side by Delegates.vetoable(Side.n) { prop, sideOld, sideNew -> sideNew != Side.n }
    }
}

class Mine(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val stager = injectValue<Stager>()
        val flats = injectFlats().value
        val tls = r.tlsFlatOwn("mine")
        flater.add(tls.neut, TpFlat.special) { it.data(Mine(tls)) }
        stager.onEndTurn {
            val gold = flats().by<Mine>().filter { it.second.side == stager.sideTurn() }.size
            //            builder.plusGold(stager.sideTurn(),gold)
        }
    }

    private class Mine(tls: TlsFlatOwn) : DataPoint(tls)
}

class Hospital(r: Resource) {
    init {
        val flater = injectValue<Flater>()
        val tls = r.tlsFlatOwn("hospital")
        flater.add(tls.neut, TpFlat.special) { it.data(Hospital(tls)) }
    }

    private class Hospital(tls: TlsFlatOwn) : DataPoint(tls)
}