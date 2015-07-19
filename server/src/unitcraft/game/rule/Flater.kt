package unitcraft.game.rule

import unitcraft.game.*
import unitcraft.land.Land
import unitcraft.land.TpFlat
import unitcraft.server.Side
import unitcraft.server.init
import java.time.Instant
import java.util.*
import kotlin.properties.Delegates

class Flater(val r:Resource,val stager: Stager,val allData:() -> AllData,val drawer:Drawer,val editor:Editor){
    val tilesEditor = ArrayList<Tile>()
    val creates = ArrayList<(Flat)->Unit>()
    val landTps = HashMap<TpFlat,MutableList<(Flat)->Unit>>()
    fun flats() = allData().flats

    init{
        editor.onEdit(PriorDraw.flat,tilesEditor,{pg,side,num->
            flats().remove(flats()[pg])
            val flat = Flat(Singl(pg))
            creates[num](flat)
            flats().add(flat)
        },{pg-> false})
        drawer.drawFlats.add{flat,side,ctx ->
            if(flat.has<DataTile>()){
                ctx.drawTile(flat.head(),flat<DataTile>().tile(side))
            }
        }
        stager.onEndTurn {
            for ((flat,point) in allData().flats.by<DataPoint>())
                for (voin in allData().objs)
                    if (flat.shape.pgs.intersect(voin.shape.pgs).isNotEmpty())
                        point.side = voin.side
        }
    }
    fun add(tileEditor:Tile,tpLand:TpFlat, create:(Flat)->Unit) {
        tilesEditor.add(tileEditor)
        creates.add(create)
        landTps.getOrPut(tpLand){ArrayList<(Flat)->Unit>()}.add(create)
    }
}

abstract class DataTile:Data{
    abstract fun tile(sideVid:Side):Tile
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

fun randomTile(tiles:List<Tile>) = tiles[Any().hashCode()%tiles.size()]

open class DataTileFix(val tile:Tile) : DataTile(){
    override fun tile(sideVid:Side) = tile
}

class Forest(r:Resource,val flater:Flater,mover:Mover,flats:()->Flats){
    init{
        val tiles = r.tlsList(4,"forest",Resource.effectPlace)
        flater.add(tiles[0],TpFlat.wild){it.data(DataForest(randomTile(tiles)))}
        mover.slotHide.add { flats()[it.head()].has<DataForest>()}
        mover.slotMoveAfter.add{ shapeFrom,move -> mover.rehide() }
    }
    class DataForest(tile:Tile) : DataTileFix(tile)
}

class Grass(r:Resource,val flater:Flater){
    init{
        val tiles = r.tlsList(5,"grass",Resource.effectPlace)
          flater.add(tiles[0],TpFlat.none){it.data(DataGrass(randomTile(tiles)))}
    }
    class DataGrass(tile:Tile) : DataTileFix(tile)
}

class Water(r:Resource,val flater:Flater){
    init{
        val tiles = r.tlsList(1,"water",Resource.effectPlace)
        flater.add(tiles[0],TpFlat.liquid){it.data(DataWater(randomTile(tiles)))}
    }
    class DataWater(tile:Tile) : DataTileFix(tile)
}

/** если юнит стоит на катапульте, то он может прыгнуть в любую проходимую для него точку */
class Catapult(val r: Resource, val flater: Flater, val spoter: Spoter, val mover: Mover, val flats: () -> Flats) : Skil {
    val tlsAkt = r.tlsAkt("catapult")

    init {
        val tile = r.tile("catapult")
        val catapult = Catapult(tile)
        flater.add(tile,TpFlat.special){it.data(catapult)}

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


    private class Catapult(tile:Tile) : DataTileFix(tile)
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

abstract class DataPoint(val tls: TlsFlatOwn):DataTile(){
    var side = Side.n
    override fun tile(sideVid:Side)=tls(sideVid,side)
}

class Flag(r: Resource,val flater: Flater) {
    init {
        val tls = r.tlsFlatOwn("flag")
        flater.add(tls.neut,TpFlat.flag){it.data(Flag(tls))}
    }
    private class Flag(tls: TlsFlatOwn) : DataPoint(tls)
}

class Mine(r: Resource,val flater: Flater,val stager: Stager,val builder:Builder, val flats: () -> Flats) {
    init {
        val tls = r.tlsFlatOwn("mine")
        flater.add(tls.neut,TpFlat.special){it.data(Mine(tls))}
        stager.onEndTurn {
            val gold = flats().by<Mine>().filter{it.second.side == stager.sideTurn()}.size()
//            builder.plusGold(stager.sideTurn(),gold)
        }
    }
    private class Mine(tls: TlsFlatOwn) : DataPoint(tls)
}

class Hospital(r: Resource, val flater: Flater) {
    init {
        val tls = r.tlsFlatOwn("hospital")
        flater.add(tls.neut,TpFlat.special){it.data(Hospital(tls))}
    }
    private class Hospital(tls: TlsFlatOwn) : DataPoint(tls)
}