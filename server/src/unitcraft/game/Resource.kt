package unitcraft.game

import org.json.simple.JSONAware
import unitcraft.server.idxOfFirst
import unitcraft.server.idxsMap
import java.awt.Color
import java.util.*

class Resource {

    private val sections = ArrayList<SectionRule>()

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

    fun htmlRule() = HtmlRule(sections).html()

    fun <T : Aide> slot(title: String) = Slot<T>(title).apply { sections.add(this) }
    fun <T : Aide> slop(title: String) = Slop<T>(title).apply { sections.add(this) }

    fun tlsVoin(name: String) = TlsObj(
            neut = tile(name, effectNeut),
            allyMyTurn = tile(name, effectAllyMyTurn),
            ally = tile(name, effectAlly),
            allyNeedTire = tile(name, effectAllyNeedTire),
            allyTire = tile(name, effectAllyTire),
            enemy = tile(name, effectEnemy),
            enemyTire = tile(name, effectEnemyTire),
            join = TlsBool(tile(name, effectBlue), tile(name, effectYelw))
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

        fun effectObjLight(h: Int, s: Int, b: Int) = Effect("objLight($h,$s,$b)") {
            fit()
            extendBottom()
            light(h, s, b)
        }

        val effectStandard = Effect("standard") {
            fit()
            extend()
        }

        val effectNeut = effectObjLight(0, 0, 40)

        val effectAllyMyTurn = effectObjLight(120, 100, 100)
        val effectAlly = effectObjLight(120, 100, 75)
        val effectAllyNeedTire = effectObjLight(120, 0, 100)
        val effectAllyTire = effectObjLight(180, 100, 100)

        val effectEnemy = effectObjLight(360, 100, 100)
        val effectEnemyTire = effectObjLight(280, 100, 100)

        val effectBlue = effectObjLight(240, 100, 100)
        val effectYelw = effectObjLight(60, 100, 100)

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

class TlsObj(val neut: Tile,
             val allyMyTurn: Tile,
             val ally: Tile,
             val allyNeedTire: Tile,
             val allyTire: Tile,
             val enemy: Tile,
             val enemyTire: Tile,
             val join: TlsBool)

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