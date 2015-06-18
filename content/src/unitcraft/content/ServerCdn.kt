package unitcraft.content

import fi.iki.elonen.NanoHTTPD
import unitcraft.game.*
import unitcraft.game.rule.CdxPlace
import unitcraft.server.Err
import org.json.simple.JSONValue
import org.omg.CORBA.portable.Delegate
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.HashMap
import java.util.HashSet
import javax.imageio.ImageIO
import kotlin.platform.platformStatic
import kotlin.properties.Delegates
import java.awt.image.BufferedImage as Image

fun main(args: Array<String>) {
    ServerCdn().deploy(args.size()==0)
}

class ServerCdn() : NanoHTTPD(8000) {
    val dirCdn = File("content/cdn/")

    val res = Resource()
    val panels = File(dirPanels).files().map{it.nameBase}

    val imgsTile = HashMap<String, BufferedImage>()
    val imgsPanel = HashMap<String, BufferedImage>()

    var maskPlace = ImageIO.read(File(dirTiles + "maskPlace.png"))
    val qdmnsTileUpdated = HashSet<Int>()
    val qdmnsPanelUpdated = HashSet<Int>()

    init{
        if(listQdmnTile.any{it%2!=0}) throw Err("qdmnTile must be even")
        res.createRules(Unitcraft.kCdxs)
        loadImgsTile()
        loadImgsPanel()
    }

    override fun start(){
        File(dirCdn,"png/").mkdirs()
        prepareJsServer(dirCdn,"ws://localhost:8080")
        WatchDir(listOf(File(dirTiles), File(dirPanels))){ file ->
            if(file.directory.name == "tiles") {
                if (file.name == "maskPlace.png") {
                    maskPlace = ImageIO.read(file)
                } else {
                    imgsTile[file.nameBase] = ImageIO.read(file)!!
                }
                qdmnsTileUpdated.clear()
            }else{
                imgsPanel[file.nameBase] = ImageIO.read(file)!!
                qdmnsPanelUpdated.clear()
            }
        }
        super.start()
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response? {
        val file = File(dirCdn, if(session.getUri()== "/") "/index.html" else session.getUri())
        println(file)
        val qdmnTile = qdmnFromFile(file,"tile")
        if(qdmnTile!=null && qdmnTile !in qdmnsTileUpdated) createTileset(qdmnTile,dirCdn)
        val qdmnPanel = qdmnFromFile(file,"panel")
        if(qdmnPanel!=null && qdmnPanel !in qdmnsPanelUpdated) createPanelset(qdmnPanel,dirCdn)

        return NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, Files.probeContentType(file.toPath()), file.inputStream());
    }

    private fun qdmnFromFile(file:File,prefix:String) =
            if(file.name.startsWith(prefix)) file.nameBase.substring(prefix.length()).toInt() else null

    private fun loadImgsTile(){
        val namesUnused = File(dirTiles).files().map { it.name }.toArrayList()
        namesUnused.remove("maskPlace.png")
        for (tile in res.tiles) {
            println(tile)
            val file = fileFromTile(tile)
            if (!file.isFile()) throw Err("Tile ${tile.name} not found")
            imgsTile[tile.name] = ImageIO.read(file)!!
            namesUnused.remove(file.name)
        }
        println("Unused:")
        for (name in namesUnused) println(name)
    }

    private fun loadImgsPanel(){
        for(file in File(dirPanels).files()) {
            imgsPanel[file.nameBase] =  ImageIO.read(file)!!
        }
    }

    private fun prepareJsServer(dirOut:File,urlWs:String) {
        val sb = StringBuilder()

        sb.appendln("""var urlWs = "$urlWs";""")
        sb.appendln()

        listQdmnPanel.joinTo(sb,",","var listQdmnPanel = [","];");
        sb.appendln()

        listQdmnTile.joinTo(sb,",","var listQdmnTile = [","];");
        sb.appendln()
        sb.appendln()

        val edges = listOf(DabTile(res.tileEdgeTurn),DabTile(res.tileEdgeWait))
        sb.appendln("var dabsEdge ="+ JSONValue.toJSONString(edges)+";")
        sb.appendln("var dabFocus ="+ DabTile(res.tileFocus).toJSONString()+";")
        sb.appendln()

        res.hintTiles.map{"function(ctx,rTile){${it.script}}"}.joinTo(sb,",\n","var hintTile = [","];");
        sb.appendln()
        sb.appendln()

        res.hintTexts.map{"function(ctx,rTile){${it.script}}"}.joinTo(sb,",\n","var hintText = [","];");
        sb.appendln()
        sb.appendln()

        val files = File(dirPanels).files()

        files.withIndex().map{it.value.nameBase+":"+it.index}.joinTo(sb,",","var imgPanels = {","};");
        sb.appendln();
        File(dirOut, "server.js").writeText(sb.toString(), "UTF-8")
    }

    private fun fileFromTile(tile:Tile) = File(dirTiles + tile.name + ".png")

    private fun createTileset(qdmn: Int,dirOut:File){
        val img = BufferedImage(50 * qdmn * 2, (res.tiles.size() / 50 + (if (res.tiles.size() % 50 > 0) 1 else 0)) * qdmn * 2, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()

        for ((ind, tile) in res.tiles.withIndex()) {
            val ctx = CtxEffectImpl(imgsTile[tile.name]!!, qdmn, maskPlace)
            ctx.(tile.effect.op)()
            g.drawImage(ctx.img, ind % 50 * qdmn * 2, ind / 50 * qdmn * 2, null)
        }
        g.dispose()
        ImageIO.write(img, "png", File(dirOut, "png/tile${qdmn}.png"))
        qdmnsTileUpdated.add(qdmn)
        println("tileset ${qdmn} updated")
    }

    private fun createPanelset(qdmn: Int,dirOut:File){
        val img = BufferedImage(panels.size() * qdmn, qdmn, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        for((idx,panel) in panels.withIndex()){
            g.drawImage(CtxEffectImpl.resize(imgsPanel[panel],qdmn), idx*qdmn, 0, null)
        }
        ImageIO.write(img, "png", File(dirOut, "png/panel${qdmn}.png"))
        qdmnsPanelUpdated.add(qdmn)
        println("panelset ${qdmn} updated")
    }

    fun deploy(isTest:Boolean) {
        val dirPrepare =  File("githubpages/"+if(isTest) "test" else "")
        dirPrepare.walkBottomUp().filter { it.name != ".git" && (isTest || it.name != "test") }.forEach {
            if(it!=dirPrepare) it.delete()
        }
        dirPrepare.mkdirs()
        File(dirPrepare,"png/").mkdirs()

        prepareJsServer(dirPrepare,"ws://"+(if(isTest) "test" else "main")+"-unitcraft.rhcloud.com:8000")
        for (qdmn in listQdmnTile) createTileset(qdmn,dirPrepare)
        for (qdmn in listQdmnPanel) createPanelset(qdmn,dirPrepare)
        val pathPrepare = dirPrepare.toPath()
        val pathCdn = dirCdn.toPath()
        dirCdn.walkTopDown().filter { it.name != "png" && it.name != "server.js"}.forEach {
            val src = it.toPath()
            if(src != pathCdn) Files.copy(src, pathPrepare.resolve(pathCdn.relativize(src)))
        }
        deployOpenshift(isTest)
    }

    companion object{
        val dirTiles = "content/data/tiles/"
        val dirPanels = "content/data/panels/"

        val listQdmnTile = listOf(44,48,52,56,60,70,72,74,76,78,80,82,84,86,88,90,92,94,96)
        val listQdmnPanel = listOf(100,120,130,140,150,160,170,180,190)

        platformStatic fun main(args: Array<String>) {
            ServerCdn().start()
            Thread.sleep(Long.MAX_VALUE)
        }
    }
}

fun File.files() = listFiles { it.isFile() }!!

val regexNameBase = "[.][^.]+$".toRegex()

val File.nameBase : String
    get() = name.replaceFirst(regexNameBase, "")

fun deployOpenshift(isTest:Boolean){
    val dirDeploy = Paths.get("openshift"+if(isTest) "Test" else "")
    var dirActionHooks = Files.createDirectories(dirDeploy.resolve(".openshift/action_hooks/"))
    dirActionHooks.resolve("start").toFile().writeText(if(isTest) scriptStartOpenshift else scriptStartOpenshiftTest)
    dirActionHooks.resolve("stop").toFile().writeText(scriptStopOpenshift)
    Files.copy(Paths.get(".idea/out/unitcraft.jar"),dirDeploy.resolve("unitcraft.jar"),StandardCopyOption.REPLACE_EXISTING)
}

val scriptStartOpenshift = """#!/bin/bash
nohup java -server -jar ${"$"}OPENSHIFT_REPO_DIR/unitcraft.jar > ${"$"}OPENSHIFT_DATA_DIR/start.log 2>&1 &"""

val scriptStartOpenshiftTest = """#!/bin/bash
nohup java -server -jar ${"$"}OPENSHIFT_REPO_DIR/unitcraft.jar test > ${"$"}OPENSHIFT_DATA_DIR/start.log 2>&1 &"""

val scriptStopOpenshift = """#!/bin/bash
if [ -z "$(ps -ef | grep unitcraft | grep -v grep)" ]
then
    echo "Server is already stopped" > ${"$"}OPENSHIFT_DATA_DIR/stop.log 2>&1
else
    kill `ps -ef | grep unitcraft | grep -v grep | awk '{ print $2 }'` > ${"$"}OPENSHIFT_DATA_DIR/stop.log 2>&1
fi"""