package unitcraft.lander

import javafx.application.Application
import javafx.beans.Observable
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.VPos
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Color.*
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import unitcraft.game.Pg
import unitcraft.game.Pgser
import unitcraft.lander.TpFlat.*
import unitcraft.lander.TpObj.std
import unitcraft.lander.TpPrism.*
import unitcraft.server.Side
import java.util.*


class Player : Application() {

    val pgser = Pgser(10, 10)
    val maxTpFlat = mapOf(special to 5, liquid to 1, wild to 2)
    val maxTpObj = mapOf(std to 7)
    val random = Random(System.currentTimeMillis())

    val exc = HashSet<Pg>()

    val primt = SimpleObjectProperty<Primt>()
    val land = SimpleObjectProperty<Land>()

    val canvas = MyCanvas()

    inner class MyCanvas : Canvas() {
        var qdmn = 0.0
        var arc = 0.0
        var mg = 0.0
        var mg2 = 0.0
        var smObj = qdmn * 10
        lateinit var g: GraphicsContext

        init {
            widthProperty().addListener { evt -> redraw() }
            heightProperty().addListener { evt -> redraw() }
            primt.addListener { evt: Observable -> redraw() }
            land.addListener { evt: Observable -> redraw() }
        }

        fun redraw() {
            g = graphicsContext2D
            g.textAlign = TextAlignment.CENTER
            g.textBaseline = VPos.CENTER
            g.fill = Color.gray(0.15)
            g.fillRect(0.0, 0.0, width, height)

            qdmn = qdmn()
            arc = qdmn / 5
            mg = qdmn / 50
            mg2 = mg * 1.5
            smObj = qdmn / 10

            if (isPrimt) {
                val map = primt.get().map
                for (pg in pgser) {
                    g.stroke = WHITE
                    g.strokeRoundRect(mg + pg.x * qdmn, mg + pg.y * qdmn, qdmn - 2 * mg, qdmn - 2 * mg, arc, arc)
                    map[pg]?.color()?.let { drawPg(pg, it) }
                }
            } else {
                val flats = land.get().flats
                val objs = land.get().objs

                for (pg in pgser) {
                    g.stroke = Color.gray(0.30)
                    g.lineWidth = mg
                    g.strokeRoundRect(mg2 + pg.x * qdmn, mg2 + pg.y * qdmn, qdmn - 2 * mg2, qdmn - 2 * mg2, arc, arc)
                    val flat = flats[pg]!!
                    if (flat.tpFlat != none) {
                        drawPg(pg, flat.color())
                        if (flat.tpFlat == special) {
                            g.stroke = flat.side.color()
                            g.lineWidth = mg * 4
                            val sm = mg * 2
                            g.strokeRoundRect(sm + pg.x * qdmn, sm + pg.y * qdmn, qdmn - 2 * sm, qdmn - 2 * sm, arc, arc)
                        }
                    }
                    val obj = objs[pg]
                    if(obj!=null) {
                        g.fill = obj.color()
                        g.fillOval(smObj + pg.x * qdmn, smObj + pg.y * qdmn, qdmn - 2 * smObj, qdmn - 2 * smObj)
                        g.fill = obj.side.vs.color()
                        g.font = Font.font(mg * 20)
                        g.fillText("" + obj.num, pg.x * qdmn + qdmn / 2, pg.y * qdmn + qdmn / 2)
                    }
                    if (flat.tpFlat != none) {
                        g.fill = flat.color().invert()
                        g.font = Font.font(mg * 15)
                        g.fillText("" + flat.num, pg.x * qdmn + qdmn*0.8, pg.y * qdmn + qdmn*0.8)
                    }
                }
            }
        }

        fun drawPg(pg: Pg, color: Color) {
            g.fill = color
            g.fillRoundRect(mg + pg.x * qdmn, mg + pg.y * qdmn, qdmn - 2 * mg, qdmn - 2 * mg, arc, arc)
        }

        fun pgFromClick(xMouse: Double, yMouse: Double) = pgser.pgOrNull((xMouse / qdmn()).toInt(), (yMouse / qdmn()).toInt())


        fun qdmn() = Math.min(width / pgser.xr, height / pgser.yr).toDouble()

        override fun isResizable() = true
        override fun prefWidth(height: Double) = width
        override fun prefHeight(width: Double) = height
    }


    override fun start(primaryStage: Stage) {
        update()
        primaryStage.title = "" + pgser.xr + "x" + pgser.yr

        val bp = StackPane()
        bp.children.add(canvas)
        canvas.widthProperty().bind(bp.widthProperty());
        canvas.heightProperty().bind(bp.heightProperty());

        val scene = Scene(bp, 1000.0, 1000.0)
        scene.setOnKeyPressed {
            if (it.code == KeyCode.SPACE) update()
        }
        if (isPrimt) {
            var pgLast: Pg? = null
            fun updPgLast(pgNew: Pg?) {
                pgNew?.let {
                    if (it in exc) exc.remove(it) else exc.add(it)
                    update()
                }
                pgLast = pgNew
            }
            canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) {
                val pgNew = canvas.pgFromClick(it.x, it.y)
                if (pgNew != pgLast) updPgLast(pgNew)
            }

            canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) {
                updPgLast(canvas.pgFromClick(it.x, it.y))
            }
        }

        primaryStage.scene = scene
        primaryStage.show()
    }

    fun update() {
        if (isPrimt) primt.set(moldPrimt(random, pgser, exc))
        else land.set(moldLand(random, pgser, maxTpFlat, maxTpObj))

    }

    companion object {
        var isPrimt = true
        lateinit var moldPrimt: MoldPrimt
        lateinit var moldLand: MoldLand
    }
}

fun play(primt: MoldPrimt) {
    Player.moldPrimt = primt
    Application.launch(Player::class.java)
}

fun play(moldLand: MoldLand) {
    Player.isPrimt = false
    Player.moldLand = moldLand
    Application.launch(Player::class.java)
}

fun Side.color() = if (this == Side.a) BLUE else YELLOW

fun ObjLand.color() = side.color()

fun FlatLand.color() =
        when (tpFlat) {
            none -> DARKGRAY
            liquid -> AQUA
            wild -> GREEN
            special -> DARKRED
            flag -> SALMON
        }

fun TpPrism.color() =
        when (this) {
            exc -> GREY
            lay -> MEDIUMAQUAMARINE
            aux -> GREEN
            three -> YELLOWGREEN
        }