package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.server.idxOfFirst
import unitcraft.server.idxsMap
import java.awt.Color
import java.util.*

class Resource {

    private val slots = ArrayList<Slot<*>>()

    val resTiles = ArrayList<ResTile>()
    val hintTiles = ArrayList<ResHintTile>()
    val hintTexts = ArrayList<ResHintText>()
    val hintTileAktOff = hintTile("ctx.globalAlpha = 0.6")

    val hintTilesTurn = mapOf(
            Dr.up to null,
            Dr.rt to hintTile("ctx.translate(rTile,0);ctx.rotate(Math.PI/2);"),
            Dr.dw to hintTile("ctx.translate(rTile,rTile);ctx.rotate(Math.PI);"),
            Dr.lf to hintTile("ctx.translate(0,rTile);ctx.rotate(-Math.PI/2);")
    )

    val tlsAktMove = tileAkt("move")

    //fun tlsFlatOwn(name:String) = TlsFlatOwn(tile(name,effectControlAlly),tile(name,effectControlEnemy),tile(name,effectControlYelw),tile(name,effectControlBlue))

    fun htmlRule() = HtmlRule(slots).html()

    fun <T:Aide> slot(title: String) = Slot<T>(title).apply { slots.add(this) }

    fun tlsVoin(name: String) = TlsObj(
            tile(name, effectNeut),
            tile(name, effectFriend),
            tile(name, effectEnemy),
            TlsBool(tile(name, effectBlue), tile(name, effectYelw))
    )

    fun tileAkt(name: String, fix: String = "akt") = tile("$name.$fix", effectAkt)
    fun tlsList(qnt: Int, name: String, effect: Effect = effectStandard) = idxsMap(qnt) { tile(name + "." + it, effect) }
    fun tlsBool(nameTrue: String, nameFalse: String, effect: Effect = effectStandard) = TlsBool(tile(nameTrue, effect), tile(nameFalse, effect))

    fun tile(tile: String, effect: Effect = effectStandard) = Tile(resTiles.idxOfOrAdd(ResTile(tile, effect)))

    fun hintTile(hintTile: String) = HintTile(hintTiles.idxOfOrAdd(ResHintTile(hintTile)))

    fun hintText(hintText: String) = HintText(hintTexts.idxOfOrAdd(ResHintText(hintText)))

    companion object {
        fun <E> MutableList<E>.idxOfOrAdd(elem: E): Int {
            val idx = this.idxOfFirst { it == elem }
            return if (idx == null) {
                this.add(elem)
                this.lastIndex
            } else {
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

        val effectEnemy = Effect("enemy") {
            fit()
            extendBottom()
            light(360, 90, 90)
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
}

class TlsObj(val neut: Tile, val ally: Tile, val enemy: Tile, val join: TlsBool)

class TlsBool(val tileTrue: Tile, val tileFalse: Tile) {
    operator fun invoke(b: Boolean) = if (b) tileTrue else tileFalse
}

data class ResTile(val name: String, val effect: Effect) {
    override fun toString() = name + "+" + effect.name
}

data class Tile(val num: Int) : JSONAware {
    override fun toJSONString() = num.toString()
}

data class ResHintTile(val script: String)
data class ResHintText(val script: String)

data class HintTile(val num: Int) : JSONAware {
    override fun toJSONString() = num.toString()
}

data class HintText(val num: Int) : JSONAware {
    override fun toJSONString() = num.toString()
}

class Effect(val name: String, val op: CtxEffect.() -> Unit) {
    override fun toString(): String {
        return "Effect($name)"
    }
}

interface CtxEffect {
    fun fit()
    fun extend()
    fun extendBottom()
    fun light(h: Int, s: Int, b: Int)
    fun place()
    fun shadow(color: Color)
    fun opacity(procent: Int)
}