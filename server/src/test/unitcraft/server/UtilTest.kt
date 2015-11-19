package unitcraft.server

import org.json.simple.JSONObject
import java.util.*
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.test.assertEquals

fun assertViolation(f: () -> Unit) {
    try {
        f()
        fail("expected Violation")
    } catch(ex: Violation) {
    }
}

fun assertErr(f: () -> Unit) {
    try {
        f()
        fail("expected Err")
    } catch(ex: Err) {
    }
}

fun <T> measure(f: () -> T): T {
    var start = System.nanoTime()
    val r = f()
    println("" + (System.nanoTime() - start) / 1000000L + "ms")
    return r
}

class LogTest : Log {
    var last: String = ""

    override fun log(event: String) {
        last = event
    }

    fun assertLast(event: String){
        assertTrue(last.startsWith(event))
    }
}

class WserTest:Wser{
    var idSsnLast = ""
    var msgLast = ""

    var idSsnPrev = ""
    var msgPrev = ""

    var closed:String? = null

    override fun onOpen(fn: (String) -> Unit) {

    }

    override fun onMsg(fn: (String, String) -> Unit) {

    }

    override fun onClose(fn: (String) -> Unit) {

    }

    override fun start() {
        throw UnsupportedOperationException()
    }

    override fun close(key: String) {
        closed = key
    }

    override fun send(key: String, msg: String) {
        idSsnPrev = idSsnLast
        msgPrev = msgLast
        idSsnLast = key
        msgLast = msg
    }
    fun assertLast(idSsn:String, msg:String){
        assertTrue(idSsnLast == idSsn && msg == msgLast,"send $msgLast to $idSsnLast but expect $msg to $idSsn")
    }
    fun assertLastOrPrev(idSsn:String, msg:String){
        assertTrue(idSsnPrev == idSsn && msg == msgPrev || idSsnLast == idSsn && msg == msgLast,"send $msgLast/$msgPrev to $idSsnLast/$idSsnPrev but expect $msg to $idSsn")
    }

    fun assertClose(key:String){
        assertEquals(key,closed)
    }
}

class GameDataStub:GameData

class CmderStub : CmderGame {
    var timesReseted = 0
    val sides = ArrayList<Side>()
    val cmds = ArrayList<String>()

    override fun createData(mission: Int?, canEdit: Boolean)=GameDataStub()

    override fun reset():GameState {
        timesReseted += 1
        cmds.clear()
        return state()
    }

    override fun cmd(side: Side, cmd: String):GameState {
        sides.add(side)
        cmds.add(cmd)
        if (cmd == "errCmd") throw Err("errCmd")
        if (cmd == "violationCmd") throw Violation("violationCmd")
        if (cmd == "needSwap") return state(SwapSide.usual)
        if (cmd == "needSwapIfRobot") return state(SwapSide.ifRobot)
        return state()
    }

    override fun cmdRobot(sideRobot: Side): String? {
        if (cmds.lastOrNull() == "errRobot") throw Err("errRobot")
        return if (cmds.lastOrNull() == "robot") "robotAnswer" else null
    }

    override fun land(): String {
        throw UnsupportedOperationException()
    }

    private fun state(swapSide: SwapSide?=null): GameState {
        if (cmds.lastOrNull() == "errState") throw Err("errState")
        return GameState(if (cmds.lastOrNull() == "win") Side.a else null, mapOf(Side.a to JSONObject(), Side.b to JSONObject()), null,swapSide)
    }

    fun assertReseted() {
        assertTrue(timesReseted == 1, "игра не сброшена")
    }
}