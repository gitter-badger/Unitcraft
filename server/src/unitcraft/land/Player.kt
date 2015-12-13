package unitcraft.land

import javafx.application.Application
import javafx.beans.Observable
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import unitcraft.game.Pg
import unitcraft.game.Pgser
import java.util.*


class Player : Application() {

    val pgser = Pgser(10, 10)
    val random = Random(System.currentTimeMillis())

    val exc = ArrayList<Pg>()

    val pgsPrimitive = SimpleObjectProperty<CtxPrism>()

    val canvas = MyCanvas()

    inner class MyCanvas : Canvas() {
        var qdmn = 0.0
        var arc = 0.0
        var mg = 0.0
        lateinit var g: GraphicsContext

        init {
            widthProperty().addListener { evt -> redraw() }
            heightProperty().addListener { evt -> redraw() }
            pgsPrimitive.addListener { evt: Observable -> redraw() }
        }

        fun redraw() {
            g = graphicsContext2D
            g.fill = Color.BLACK
            g.fillRect(0.0, 0.0, width, height)
            qdmn = qdmn()
            arc = qdmn / 5
            mg = qdmn / 100

            val map = pgsPrimitive.get().map

            g.stroke = Color.WHITE
            for (pg in pgser) {
                g.strokeRoundRect(mg + pg.x * qdmn, mg + pg.y * qdmn, qdmn - 2 * mg, qdmn - 2 * mg, arc, arc)
                when(map[pg]) {
                    TpPrism.exc -> drawPg(pg,Color.GREY)
                    TpPrism.one -> drawPg(pg,Color.LIGHTGREEN)
                }
            }
        }

        fun drawPg(pg: Pg,color:Color) {
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

        primaryStage.scene = scene
        primaryStage.show()
    }

    fun update() {
        pgsPrimitive.set(ctxPrism(random,pgser, exc))
    }

    companion object {
        lateinit var ctxPrism: (Random, Pgser, List<Pg>) -> CtxPrism

        fun start(ctxPrism: (Random, Pgser, List<Pg>) -> CtxPrism) {
            this.ctxPrism = ctxPrism
            Application.launch(Player::class.java)
        }
    }
}

enum class TpPrism {
    exc, one, two, three
}

class CtxPrism(val random: Random, val pgser: Pgser, exc: List<Pg>) {

    val map = HashMap<Pg, TpPrism>().apply {
        exc.forEach { this[it] = TpPrism.exc }
    }

    fun <E> rnd(list: List<E>) = if (list.isEmpty()) null else list[random.nextInt(list.size)]

    fun all() = pgser.pgs

    fun isExc(pg: Pg) = map[pg] == TpPrism.exc
    fun isLay(pg: Pg) = map[pg] == TpPrism.one
    fun lay(pg: Pg) {
        if (!isExc(pg)) map[pg] = TpPrism.one
    }

}

fun prism(fn: CtxPrism.() -> Unit): (Random, Pgser, List<Pg>) -> CtxPrism = { r, pgser, exc ->
    val prism = CtxPrism(r, pgser, exc)
    prism.fn()
    prism
}


val splash = prism {
    var i = 0
    while (i < 10) {
        val pg = rnd(all().filterNot { isExc(it) || isLay(it) }) ?: break
        lay(pg)
        i += 1
    }
}


fun main(args: Array<String>) {
    Player.start(splash)
}
