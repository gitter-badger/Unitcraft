package unitcraft.game

import unitcraft.game.rule.CdxStaziser
import java.util.ArrayList
import java.util.HashMap
import unitcraft.server.Err
import org.json.simple.JSONAware
import unitcraft.server.Side
import unitcraft.server.idxsMap
import unitcraft.game.Effect
import java.awt.Color
import kotlin.reflect.*
import kotlin.reflect.jvm.java

class Resource {
    val tiles = ArrayList<Tile>()
    val hintTiles = ArrayList<HintTile>()
    val hintTexts = ArrayList<HintText>()

    val hintTileFlip = hintTile("ctx.translate(rTile,0);ctx.scale(-1,1);")
    val hintTileDeploy = hintTile("")
    val hintTileDead = hintTile("")
    val hintTileTouch = hintTile("ctx.translate(0.3*rTile,0);ctx.translate(0.1*rTile,-0.1*rTile);ctx.scale(0.7,0.7);")
    val hintTileTurnR = hintTile("ctx.translate(rTile,0);ctx.rotate(Math.PI/2);")
    val hintTileTurnL = hintTile("")
    val hintTileTurnD = hintTile("")

    val hintTextLife = hintText("ctx.fillStyle = 'white';")
    val hintTextNeutral = hintText("ctx.fillStyle = 'yellow';")
    val hintTextPrice = hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'white';")
    val hintTextRedPrice = hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'red';")

    val tileFocus = tile("focus")
    val tileEdgeTurn = tile("edgeTurn", effectPlace)
    val tileEdgeWait = tile("edgeWait", effectPlace)
    val tlsAktMove = tlsAkt("move")

    val buildiks = ArrayList<Int>()

    fun createRules(rules:List<KClass<out Cdx>>) = rules.map{(it.java as Class<out Cdx>).getConstructor(javaClass<Resource>()).newInstance(this)}

    fun tlsVoin(name:String) = TlsVoin(tile(name,effectFriend),tile(name,effectEnemy),tile(name,effectNeut))
    fun tlsAkt(name:String) = TlsAkt(tile(name+".akt",effectAkt),tile(name+".akt",effectAktOff))
    fun tlsList(qnt: Int, name: String,effect: Effect =effectStandard) = idxsMap(qnt){tile(name+"."+it,effect)}

    fun tile(tile: String, effect: Effect = effectStandard):Int {
        val t = Tile(tile, effect)
        val idx = tiles.indexOf(t)
        if(idx==-1){
            tiles.add(t)
            return tiles.lastIndex
        }else{
            return idx
        }
    }

    fun hintTile(hintTile: String):Int {
        hintTiles.add(HintTile(hintTile))
        return hintTiles.size()-1
    }

    fun hintText(hintText: String):Int {
        hintTexts.add(HintText(hintText))
        return hintTexts.size()-1
    }

    fun buildik(tile: Int):Int {
        buildiks.add(tile)
        return buildiks.size()-1
    }

    companion object{
        val effectStandard = Effect("standard") {
            fit()
            extend()
        }

        val effectFriend = Effect("friend") {
            fit()
            extendBottom()
            light(Color(50, 255, 50))
        }

        val effectNeut = Effect("neut") {
            fit()
            extendBottom()
        }

        val effectEnemy = Effect("enemy") {
            fit()
            extendBottom()
            light(Color(255, 50, 50))
        }

        val effectAkt = Effect("akt") {
            fit()
            extend()
            shadow(Color.black)
        }

        val effectAktOff = Effect("aktOff") {
            fit()
            extend()
            shadow(Color.red)
        }
        val effectPlace = Effect("place") {
            place()
        }
    }
//    companion object {
//        val prmsTestAdd = ArrayList<Any>()
//        val numTiles = HashMap<String, Int>()
//        val numHintTile = HashMap<String, Int>()
//        val numHintText = HashMap<String, Int>()
//
//
//
//        init {
//            val r = Resource()
//            for (i in r.tiles.indices) numTiles[r.tiles[i].key] = i
//            for (i in r.hintTiles.indices) numHintTile[r.hintTiles[i].script] = i
//            for (i in r.hintTexts.indices) numHintText[r.hintTexts[i].script] = i
//            r.optsTest.mapTo(prmsTestAdd) { it.component2() }
//        }
//    }
}

class TlsVoin(val ally:Int,val enemy:Int,val neut:Int){
    fun invoke(side:Side,sideVoin: Side?) =
            if(sideVoin==null) neut
            else if(sideVoin==side) ally else enemy
}

class TlsAkt(val aktOn:Int,val aktOff:Int){
    fun invoke(isOn: Boolean) = if(isOn) aktOn else aktOff
}

data class Tile(val name: String, val effect: Effect) {
    override fun toString() = name+"+"+effect.name
}

data class HintTile(val script: String)
data class HintText(val script: String)
