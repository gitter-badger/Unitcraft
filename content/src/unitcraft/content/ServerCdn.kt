package unitcraft.content

import fi.iki.elonen.NanoHTTPD
import unitcraft.game.ResTile
import unitcraft.game.registerUnitcraft
import unitcraft.server.Err
import unitcraft.server.idxsMap
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO
import java.awt.image.BufferedImage as Image

fun main(args: Array<String>) {
    ServerCdn().deploy(args.size == 0)
}

class ServerCdn() : NanoHTTPD(8000) {
    val dirCdn = File("content/cdn/")

    val res = registerUnitcraft()
    val panels = File(dirPanels).files().map { it.nameBase }

    val imgsTile = HashMap<String, BufferedImage>()
    val imgsPanel = HashMap<String, BufferedImage>()

    var maskPlace = ImageIO.read(File(dirTiles + "maskPlace.png"))
    val qdmnsTileUpdated = HashSet<Int>()
    val qdmnsPanelUpdated = HashSet<Int>()

    init {
        if (listQdmnTile.any { it % 2 != 0 }) throw Err("qdmnTile must be even")
        loadImgsTile()
        loadImgsPanel()
    }

    override fun start() {
        File(dirCdn, "png/").mkdirs()
        prepareJsServer(dirCdn, "ws://localhost:8080")
        WatchDir(listOf(File(dirTiles), File(dirPanels))) { file ->
            for (i in 0..2) {
                try {
                    if (file.directory.name == "tiles") {
                        if (file.name == "maskPlace.png") {
                            maskPlace = ImageIO.read(file)
                        } else {
                            imgsTile[file.nameBase] = ImageIO.read(file)!!
                        }
                        qdmnsTileUpdated.clear()
                        break
                    } else {
                        imgsPanel[file.nameBase] = ImageIO.read(file)!!
                        qdmnsPanelUpdated.clear()
                        break
                    }
                } catch(ex: Exception) {
                    ex.printStackTrace()
                    Thread.sleep(100)
                }
            }
        }
        super.start()
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response? {
        val file = File(dirCdn, if (session.uri == "/") "/index.html" else session.uri)
        println(file)
        val qdmnTile = qdmnFromFile(file, "tile")
        if (qdmnTile != null && qdmnTile !in qdmnsTileUpdated) createTileset(qdmnTile, dirCdn)
        val qdmnPanel = qdmnFromFile(file, "panel")
        if (qdmnPanel != null && qdmnPanel !in qdmnsPanelUpdated) createPanelset(qdmnPanel, dirCdn)

        return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, Files.probeContentType(file.toPath()), file.inputStream());
    }

    private fun qdmnFromFile(file: File, prefix: String) =
            if (file.name.startsWith(prefix)) file.nameBase.substring(prefix.length).toInt() else null

    private fun loadImgsTile() {
        val namesUnused = File(dirTiles).files().map { it.name }.toArrayList()
        namesUnused.remove("maskPlace.png")
        for (tile in res.resTiles) {
            println(tile)
            val file = fileFromTile(tile)
            if (!file.isFile) throw Err("Tile ${tile.name} not found")
            imgsTile[tile.name] = ImageIO.read(file)!!
            namesUnused.remove(file.name)
        }
        println("Unused:")
        for (name in namesUnused) println(" " + name)
    }

    private fun loadImgsPanel() {
        for (file in File(dirPanels).files()) {
            imgsPanel[file.nameBase] = ImageIO.read(file)!!
        }
    }

    private fun prepareJsServer(dirOut: File, urlWs: String) {
        val sb = StringBuilder()

        sb.appendln("""var urlWs = "$urlWs";""")
        sb.appendln()

        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy.MM.dd.HH"))
        sb.appendln("""var version = "$now";""")
        sb.appendln()

        listQdmnPanel.joinTo(sb, ",", "var listQdmnPanel = [", "];");
        sb.appendln()

        listQdmnTile.joinTo(sb, ",", "var listQdmnTile = [", "];");
        sb.appendln()
        sb.appendln()

        res.hintTiles.map { "function(ctx,rTile){${it.script}}" }.joinTo(sb, ",\n", "var hintTile = [", "];");
        sb.appendln()
        sb.appendln()

        res.hintTexts.map { "function(ctx,rTile,txt){${it.script}}" }.joinTo(sb, ",\n", "var hintText = [", "];");
        sb.appendln()
        sb.appendln()

        val files = File(dirPanels).files()

        files.withIndex().map { it.value.nameBase + ":" + it.index }.joinTo(sb, ",", "var imgPanels = {", "};");
        sb.appendln();
        File(dirOut, "server.js").writeText(sb.toString())
        File(dirOut, "rule.html").writeText(res.htmlRule())
    }

    private fun fileFromTile(resTile: ResTile) = File(dirTiles + resTile.name + ".png")

    private fun createTileset(qdmn: Int, dirOut: File) {
        val img = BufferedImage(50 * qdmn * 2, (res.resTiles.size / 50 + (if (res.resTiles.size % 50 > 0) 1 else 0)) * qdmn * 2, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()

        for ((ind, tile) in res.resTiles.withIndex()) {
            val ctx = CtxEffectImpl(imgsTile[tile.name]!!, qdmn, maskPlace)
            ctx.(tile.effect.op)()
            g.drawImage(ctx.img, ind % 50 * qdmn * 2, ind / 50 * qdmn * 2, null)
        }
        g.dispose()
        ImageIO.write(img, "png", File(dirOut, "png/tile$qdmn.png"))
        qdmnsTileUpdated.add(qdmn)
        println("tileset $qdmn updated")
    }

    private fun createPanelset(qdmn: Int, dirOut: File) {
        val img = BufferedImage(panels.size * qdmn, qdmn, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        for ((idx, panel) in panels.withIndex()) {
            g.drawImage(CtxEffectImpl.resize(imgsPanel[panel]!!, qdmn), idx * qdmn, 0, null)
        }
        ImageIO.write(img, "png", File(dirOut, "png/panel$qdmn.png"))
        qdmnsPanelUpdated.add(qdmn)
        println("panelset $qdmn updated")
    }

    fun deploy(isTest: Boolean) {
        deployOpenshift(isTest)
        deployGithub(isTest)
    }

    fun deployGithub(isTest: Boolean) {
        val dirPrepare = File("githubpages/" + if (isTest) "test" else "")
        dirPrepare.walkBottomUp().treeFilter { it.name != ".git" && (isTest || it.name != "test") }.forEach {
            if (it != dirPrepare) it.delete()
        }
        dirPrepare.mkdirs()
        File(dirPrepare, "png/").mkdirs()

        prepareJsServer(dirPrepare, "ws://" + (if (isTest) "test" else "main") + "-unitcraft.rhcloud.com:8000")
        for (qdmn in listQdmnTile) createTileset(qdmn, dirPrepare)
        for (qdmn in listQdmnPanel) createPanelset(qdmn, dirPrepare)
        val pathPrepare = dirPrepare.toPath()
        val pathCdn = dirCdn.toPath()
        dirCdn.walkTopDown().treeFilter { it.name != "png" && it.name != "server.js" && it.name != "rule.html" }.forEach {
            val src = it.toPath()
            if (src != pathCdn) Files.copy(src, pathPrepare.resolve(pathCdn.relativize(src)))
        }
    }

    companion object {
        val dirTiles = "content/data/tiles/"
        val dirPanels = "content/data/panels/"

        val listQdmnTile = idxsMap(40) { 40 + it * 2 }
        val listQdmnPanel = idxsMap(11) { 70 + it * 10 }

        @JvmStatic fun main(args: Array<String>) {
            ServerCdn().start()
            Thread.sleep(Long.MAX_VALUE)
        }
    }
}

fun File.files() = listFiles { it.isFile }!!

val regexNameBase = "[.][^.]+$".toRegex()

val File.nameBase: String
    get() = name.replaceFirst(regexNameBase, "")

fun deployOpenshift(isTest: Boolean) {
    val dirDeploy = Paths.get("openshift" + if (isTest) "Test" else "")
    var dirActionHooks = Files.createDirectories(dirDeploy.resolve(".openshift/action_hooks/"))
    dirActionHooks.resolve("start").toFile().writeText(scriptStartOpenshift)
    dirActionHooks.resolve("stop").toFile().writeText(scriptStopOpenshift)
    val home = Paths.get(System.getProperty("user.home"))
    val libsKotlin = listOf("kotlin-reflect.jar", "kotlin-runtime.jar").map { home.resolve(".IntelliJIdea15/config/plugins/Kotlin/lib/$it") }
    val libsServer = File("server/lib").files().map { it.toPath() }
    ArrayList<Path>().apply {
        addAll(libsKotlin)
        addAll(libsServer)
        add(Paths.get(".idea/out/unitcraft.jar"))
    }.map { copyFileToDir(it, dirDeploy) }
}

fun copyFileToDir(file: Path, dir: Path) {
    val fileTo = dir.resolve(file.fileName)
    if (!Files.exists(fileTo) || Files.getLastModifiedTime(file) > Files.getLastModifiedTime(fileTo)) {
        Files.copy(file, fileTo, StandardCopyOption.REPLACE_EXISTING)
        println("updated $fileTo")
    }
}

val cp = listOf("unitcraft.jar", "json-simple-1.1.1.jar", "Java-WebSocket-1.3.0.jar", "java-image-scaling-0.8.5.jar", "kotlin-runtime.jar", "kotlin-reflect.jar")
        .map { "${"$"}OPENSHIFT_REPO_DIR/$it" }
        .joinToString(":")

val osDataDir = "\$OPENSHIFT_DATA_DIR"

val scriptStartOpenshift = """#!/bin/bash
export JAVA_HOME="/etc/alternatives/java_sdk_1.8.0"
export PATH=/etc/alternatives/java_sdk_1.8.0/bin:${"$"}PATH
nohup java -server -cp $cp unitcraft.server.MainKt > $osDataDir/start.log 2>&1 &"""

val scriptStopOpenshift = """#!/bin/bash
if [ -z "$(ps -ef | grep unitcraft | grep -v grep)" ]
then
    echo "Server is already stopped" > ${"$"}OPENSHIFT_DATA_DIR/stop.log 2>&1
else
    kill `ps -ef | grep unitcraft | grep -v grep | awk '{ print $2 }'` > ${"$"}OPENSHIFT_DATA_DIR/stop.log 2>&1
fi"""