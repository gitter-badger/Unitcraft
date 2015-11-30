package unitcraft.game

import java.util.ArrayList
import java.util.HashMap
import unitcraft.server.Err
import org.json.simple.JSONAware
import unitcraft.server.Side
import unitcraft.server.idxsMap
import unitcraft.game.Effect
import unitcraft.server.idxOfFirst
import java.awt.Color
import kotlin.reflect.*

class Resource {

    val resTiles = ArrayList<ResTile>()
    val hintTiles = ArrayList<ResHintTile>()
    val hintTexts = ArrayList<ResHintText>()

    val hintTileDeploy = hintTile("")
    val hintTileAktOff = hintTile("ctx.globalAlpha = 0.6")



    val hintTilesTurn = mapOf(
            Dr.up to null,
            Dr.rt to hintTile("ctx.translate(rTile,0);ctx.rotate(Math.PI/2);"),
            Dr.dw to hintTile("ctx.translate(rTile,rTile);ctx.rotate(Math.PI);"),
            Dr.lf to hintTile("ctx.translate(0,rTile);ctx.rotate(-Math.PI/2);")
    )



    val tlsAktMove = tileAkt("move")
    val tileHide = tile("hide")

    val tileGround = tile("ground",effectPlace)

    val grounds = listOf(tile("ground.ally",effectPlace),tile("ground.enemy",effectPlace),tile("ground.blue",effectPlace),tile("ground.yelw",effectPlace))

    //fun tlsFlatOwn(name:String) = TlsFlatOwn(tile(name,effectControlAlly),tile(name,effectControlEnemy),tile(name,effectControlYelw),tile(name,effectControlBlue))

    fun tlsVoin(name:String) = TlsObj(
            tile(name,effectNeut),
            TlsBool(tile(name,effectFriend),tile(name,effectFriendTired)),
            TlsBool(tile(name,effectEnemy),tile(name,effectEnemyTired)),
            TlsBool(tile(name,effectBlue),tile(name,effectYelw))
    )
    fun tileAkt(name:String, fix:String = "akt") = tile("$name.$fix",effectAkt)
    fun tlsList(qnt: Int, name: String,effect: Effect = effectStandard) = idxsMap(qnt){tile(name+"."+it,effect)}
    fun tlsBool(nameTrue:String,nameFalse:String,effect: Effect =effectStandard) = TlsBool(tile(nameTrue,effect),tile(nameFalse,effect))

    fun tile(tile: String, effect: Effect = effectStandard) = Tile(resTiles.idxOfOrAdd(ResTile(tile, effect)))

    fun hintTile(hintTile: String) = HintTile(hintTiles.idxOfOrAdd(ResHintTile(hintTile)))

    fun hintText(hintText: String) = HintText(hintTexts.idxOfOrAdd(ResHintText(hintText)))

    companion object{
        fun <E> MutableList<E>.idxOfOrAdd(elem:E):Int{
            val idx = this.idxOfFirst { it == elem }
            return if(idx==null){
                this.add(elem)
                this.lastIndex
            }else{
                idx
            }
        }

        val effectStandard = Effect("standard") {
            fit()
            extend()
        }

        val effectNeut = Effect("neut") {
            fit()
            extendBottom()
            light(0, 0, 50)
        }

        val effectFriend = Effect("friend") {
            fit()
            extendBottom()
            light(120, 90, 90)
        }

        val effectFriendTired = Effect("friendTired") {
            fit()
            extendBottom()
            light(120, 90, 50)
        }

        val effectEnemy = Effect("enemy") {
            fit()
            extendBottom()
            light(360, 90, 90)
        }

        val effectEnemyTired = Effect("enemyTired") {
            fit()
            extendBottom()
            light(360, 90, 50)
        }

        val effectBlue = Effect("blue") {
            fit()
            extendBottom()
            light(180, 90, 90)
        }

        val effectYelw = Effect("yelw") {
            fit()
            extendBottom()
            light(60, 90, 90)
        }

        val effectAkt = Effect("akt") {
            fit()
            extend()
            shadow(Color.black)
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

open class TlsFlatOwn(val ally:Tile,val enemy:Tile,val yellow:Tile,val blue:Tile){
    operator fun invoke(sideVid:Side, sideOwn: Side) = if(sideOwn == sideVid) ally else enemy
}

class TlsObj(val neut: Tile, val ally: TlsBool, val enemy: TlsBool, val join: TlsBool)

class TlsBool(val tileTrue:Tile,val tileFalse:Tile){
    operator fun invoke(b: Boolean) = if(b) tileTrue else tileFalse
}

data class ResTile(val name: String, val effect: Effect) {
    override fun toString() = name+"+"+effect.name
}

data class Tile(val num:Int):JSONAware{
    override fun toJSONString()=num.toString()
}

data class ResHintTile(val script: String)
data class ResHintText(val script: String)

data class HintTile(val num:Int):JSONAware{
    override fun toJSONString()=num.toString()
}

data class HintText(val num:Int):JSONAware{
    override fun toJSONString()=num.toString()
}

class Effect(val name: String, val op: CtxEffect.() -> Unit){
    override fun toString(): String {
        return "Effect($name)"
    }
}

interface CtxEffect{
    fun fit()
    fun extend()
    fun extendBottom()
    fun light(h:Int,s:Int,b:Int)
    fun place()
    fun shadow(color: Color)
    fun opacity(procent:Int)
}