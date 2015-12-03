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
    fun flats() = allData().flats
    val editor: Editor by inject()

    private val tilesEditor = ArrayList<Tile>()
    private val createsEditor = ArrayList<(Flat, Side) -> Unit>()

    private val landTps = HashMap<TpFlat, MutableList<(Flat, Side) -> Unit>>()

    val slotDrawFlat = r.slot<AideDrawFlat>("После рисования плоскуна")

    init {
        val stager = injectValue<Stager>()
        editor.onEdit(PriorDraw.flat, tilesEditor, { pg, side, num ->
            flats().remove(flats()[pg])
            val flat = Flat(pg)
            createsEditor[num](flat, side)
            flats().add(flat)
        }, { pg -> false })

        stager.slotTurnEnd.add(50,this,"захватываются плоскуны контроля")  {
            for ((flat, point) in flats().bothBy<Point>()) allData().objs[flat.pg]?.side?.let{point.side = it}
        }
        val groundSimple = r.tile("ground", Resource.effectPlace)
        val grounds = listOf(r.tile("ground.ally", Resource.effectPlace), r.tile("ground.enemy", Resource.effectPlace), r.tile("ground.blue", Resource.effectPlace), r.tile("ground.yelw", Resource.effectPlace))
        val tileFlatNull = r.tile("null.flat")

        injectValue<Drawer>().slotDraw.add(0,this,"рисует плоскунов"){
            for (flat in flats().sort()) {
                val gt = if(flat.has<Simple>()) groundSimple to flat<Simple>().tile
                else if(flat.has<Point>()){
                    val data = flat<Point>()
                    (if (stager.isBeforeTurn(side)) grounds[data.side.ordinal + 2] else if (data.side == side) grounds[0] else grounds[1]) to data.tile
                }else if(flat.has<Place>()){
                    val data = flat<Place>()
                    data.tiles[flat.hashCode() % data.tiles.size] to null
                }else groundSimple to tileFlatNull
                drawTile(flat.pg, gt.first)
                gt.second?.let { drawTile(flat.pg, it) }
                slotDrawFlat.exe(AideDrawFlat(this, flat, side))
            }
        }
    }

    fun addPoint(tile: Tile, tpFlat: TpFlat?, create: (Flat, Side) -> Unit) {
        val crt = { flat:Flat,side:Side -> create(flat,side); flat.data(Point(tile,side)) }
        tilesEditor.add(tile)
        createsEditor.add(crt)
        if(tpFlat!=null) landTps.getOrPut(tpFlat) { ArrayList<(Flat, Side) -> Unit>() }.add(crt)
    }

    fun addPlace(tiles: List<Tile>, tpFlat: TpFlat?, create: (Flat) -> Unit) {
        add(tiles.first(),tpFlat,{ flat,side -> create(flat);flat.data(Place(tiles)) })
    }

    fun addSimple(tile:Tile, tpFlat: TpFlat?, create: (Flat) -> Unit) {
        add(tile,tpFlat,{ flat,side -> create(flat);flat.data(Simple(tile)) })
    }

    fun side(flat:Flat) = flat<Point>().side

    private fun add(tile: Tile, tpFlat: TpFlat?, create: (Flat, Side) -> Unit){
        tilesEditor.add(tile)
        createsEditor.add(create)
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

    private class Point(val tile:Tile,var side:Side):Data
    private class Place(val tiles:List<Tile>):Data
    private class Simple(val tile:Tile):Data
}

class AideDrawFlat(val ctx: CtxDraw, val flat:Flat, val side: Side):Aide

class Sand(r: Resource) {
    init {
        val tiles = r.tlsList(4, "sand", Resource.effectPlace)
        injectValue<Flater>().addPlace(tiles, TpFlat.wild) { it.data(DataSand) }
    }

    object DataSand : Data
}

class Forest(r: Resource) {
    init {
        val flats = injectFlats().value
        val tiles = r.tlsList(4, "forest", Resource.effectPlace)

        injectValue<Flater>().addPlace(tiles, TpFlat.wild) { it.data(DataForest) }

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
        injectValue<Flater>().addPlace(tiles, TpFlat.none) { it.data(DataGrass) }
    }

    object DataGrass : Data
}

class Water(r: Resource) {
    init {
        val tiles = r.tlsList(3, "water", Resource.effectPlace)
        injectValue<Flater>().addPlace(tiles, TpFlat.liquid) { it.data(DataWater) }
    }

    object DataWater : Data
}

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(r: Resource) {
    init {
        val tile = r.tile("catapult")
        injectValue<Flater>().addSimple(tile, TpFlat.special) { it.data(Catapult) }

        val spoter = injectValue<Spoter>()
        val mover = injectValue<Mover>()
        val tileAkt = r.tileAkt("catapult")
        val flats = injectFlats().value
        val skil = createSkil {
            obj.pg.all.map { pg -> mover.move(obj, pg, sideVid)?.let{ akt(pg, tileAkt){
                if(it()) spoter.tire(obj)
            }}}
        }
        spoter.listSkil.add {
            if (it.pg in flats().by<Catapult>().map { it.pg }) skil else null
        }
    }

    private object Catapult : Data
}

/** если юнит стоит на крепости, то его нельзя поранить*/
class Fortress(r: Resource) {
    init {
        val tile = r.tile("fortress")
        injectValue<Flater>().addSimple(tile, TpFlat.special) { it.data(Fortress) }
        val flats = injectFlats().value
        injectValue<Lifer>().slotStopDamage.add{ obj,isPoison -> !isPoison && flats()[obj.pg].has<Fortress>()}
    }

    object Fortress : Data
}

class Flag(r: Resource) {
    val flats by injectFlats()
    val flater by inject<Flater>()
    init {
        val allData = injectAllData().value
        val tile = r.tile("flag")

        flater.addPoint(tile, TpFlat.flag) { flat, side ->
            flat.data(DataFlag)
        }

        injectValue<Stager>().slotTurnEnd.add(51,this,"игрок с меньшинством флагов теряет 1 очко")  {
            val sideLost = sideMost().vs
            allData().point[sideLost] = allData().point[sideLost]!! - 1
        }
    }

    fun sideMost():Side{
        val flags = flats().by<DataFlag>()
        return if(flags.count { flater.side(it) == Side.a }*2>flags.size) Side.a else Side.b
    }

    private object DataFlag:Data
}

class Goldmine(r: Resource) {
    init {
        val builder = injectValue<Builder>()
        val flats = injectFlats().value
        val tile = r.tile("mine")
        val flater = injectValue<Flater>()
        injectValue<Flater>().addPoint(tile, TpFlat.special) { flat, side -> flat.data(Goldmine) }
        injectValue<Stager>().slotTurnEnd.add(60,this,"захваченные золотые шахты дают золото")  {
            builder.plusGold(side, flats().by<Goldmine>().count { flater.side(it) == side })
        }
    }

    private object Goldmine : Data
}

class Hospital(r: Resource) {
    init {
        val tile = r.tile("hospital")
        injectValue<Flater>().addPoint(tile, null) { flat, side -> flat.data(Hospital) }
    }

    private object Hospital : Data
}