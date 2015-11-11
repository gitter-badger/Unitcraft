package unitcraft.game

import java.util.ArrayList
import java.util.HashMap
import unitcraft.server.Err
import org.json.simple.JSONAware
import unitcraft.server.Side
import unitcraft.server.idxsMap
import unitcraft.game.Effect
import java.awt.Color
import kotlin.reflect.*

class Resource {
    val resTiles = ArrayList<ResTile>()
    val hintTiles = ArrayList<HintTile>()
    val hintTexts = ArrayList<HintText>()
    val buildiks = ArrayList<Int>()

    val hintTileDeploy = hintTile("")
    val hintTileDead = hintTile("")
    val hintTileTouch = hintTile("ctx.translate(0.3*rTile,0);ctx.translate(0.1*rTile,-0.1*rTile);ctx.scale(0.7,0.7);")
    val hintTileTurnR = hintTile("ctx.translate(rTile,0);ctx.rotate(Math.PI/2);")
    val hintTileTurnL = hintTile("")
    val hintTileTurnD = hintTile("")

    val hintTextNeutral = hintText("ctx.fillStyle = 'yellow';")

    val hintTextRedPrice = hintText("ctx.translate(rTile,0);ctx.textAlign = 'right';ctx.fillStyle = 'red';")

    val tlsAktMove = tlsAkt("move")
    val tileHide = tile("hide")

    fun tlsFlatOwn(name:String) = TlsFlatOwn(tile(name,effectFlat),tile(name,effectControlAlly),tile(name,effectControlEnemy))
    fun tlsVoin(name:String) = TlsSolid(
            tile(name,effectNeut),
            TlsBool(tile(name,effectFriend),tile(name,effectFriendTired)),
            TlsBool(tile(name,effectEnemy),tile(name,effectEnemyTired))
    )
    fun tlsAkt(name:String,fix:String = "akt") = TlsAkt(tile("$name.$fix",effectAkt),tile("$name.$fix",effectAktOff))
    fun tlsList(qnt: Int, name: String,effect: Effect = effectStandard) = idxsMap(qnt){tile(name+"."+it,effect)}
    fun tlsBool(nameTrue:String,nameFalse:String,effect: Effect =effectStandard) = TlsBool(tile(nameTrue,effect),tile(nameFalse,effect))

    fun tile(tile: String, effect: Effect = effectStandard):Tile {
        val t = ResTile(tile, effect)
        val idx = resTiles.indexOf(t)
        if(idx==-1){
            resTiles.add(t)
            return Tile(resTiles.lastIndex)
        }else{
            return Tile(idx)
        }
    }

    fun hintTile(hintTile: String):Int {
        hintTiles.add(HintTile(hintTile))
        return hintTiles.size -1
    }

    fun hintText(hintText: String):Int {
        hintTexts.add(HintText(hintText))
        return hintTexts.size -1
    }

    fun buildik(tile: Int):Int {
        buildiks.add(tile)
        return buildiks.size -1
    }

    companion object{
        val effectStandard = Effect("standard") {
            fit()
            extend()
        }

        val effectNeut = Effect("neut") {
            fit()
            extendBottom()
        }

        val effectFriend = Effect("friend") {
            fit()
            extendBottom()
            light(Color(80, 255, 80))
        }

        val effectFriendTired = Effect("friendTired") {
            fit()
            extendBottom()
            light(Color(0, 150, 0))
        }

        val effectEnemy = Effect("enemy") {
            fit()
            extendBottom()
            light(Color(255, 80, 80))
        }

        val effectEnemyTired = Effect("enemyTired") {
            fit()
            extendBottom()
            light(Color(150, 0, 0))
        }

        val effectAkt = Effect("akt") {
            fit()
            extend()
            shadow(Color.black)
        }

        val effectAktOff = Effect("aktOff") {
            fit()
            extend()
            shadow(Color.black)
            opacity(50)
        }

        val effectPlace = Effect("place") {
            place()
        }

        val effectFlat = Effect("controlAlly") {
            fit()
            extend()
            flat(Color(150, 150, 150))
        }

        val effectControlAlly = Effect("controlAlly") {
            fit()
            extend()
            flat(Color(50, 150, 50))
        }

        val effectControlEnemy = Effect("controlEnemy") {
            fit()
            extend()
            flat(Color(150, 50, 50))
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

open class TlsFlatOwn(val neut:Tile,val ally:Tile,val enemy:Tile){
    operator fun invoke(sideVid:Side, sideOwn: Side) =
            if(sideOwn.isN) neut else if(sideOwn == sideVid) ally else enemy
}

class TlsSolid(val neut: Tile, val ally: TlsBool, val enemy: TlsBool){
    fun invoke(side:Side, sideOwn: Side,isFresh:Boolean) =
            if(sideOwn.isN) neut else if(sideOwn == side) ally(isFresh) else enemy(isFresh)
}

class TlsAkt(val aktOn:Tile,val aktOff:Tile){
    operator fun invoke(isOn: Boolean) = if(isOn) aktOn else aktOff
}

class TlsBool(val tileTrue:Tile,val tileFalse:Tile){
    operator fun invoke(b: Boolean) = if(b) tileTrue else tileFalse
}

data class ResTile(val name: String, val effect: Effect) {
    override fun toString() = name+"+"+effect.name
}

data class Tile(val num:Int):JSONAware{
    override fun toJSONString()=num.toString()
}

data class HintTile(val script: String)
data class HintText(val script: String)

class Effect(val name: String, val op: CtxEffect.() -> Unit){
    override fun toString(): String {
        return "Effect($name)"
    }
}

interface CtxEffect{
    fun fit()
    fun extend()
    fun extendBottom()
    fun light(color: Color)
    fun place()
    fun shadow(color: Color)
    fun flat(color: Color)
    fun opacity(procent:Int)
}