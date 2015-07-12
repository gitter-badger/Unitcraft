package unitcraft.server

import kotlin.properties.Delegates
import org.junit.Before
import org.junit.Test
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import kotlin.test.assertEquals
import java.util.ArrayList
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue


class TestRoom {
    val parser = JSONParser()
    var log = LogTest()
    var send = SenderTest()
    var game: CmderStub by Delegates.notNull()
    var room: Room by Delegates.notNull()
    val id = Id("0000")
    val idVs = Id("0001")
    val cmder = CmderStub()

    Before fun before() {
        game = CmderStub()
        room = Room(log, send,id,idVs,5, cmder)
    }

    // комната отсылает состояние игры
    Test fun sendState() {
        assertEquals(send.idPrev,id)
        assertEquals(send.idLast,idVs)
        checkState(0L)

        room.cmd(id,Prm("0#cmd"))

        assertEquals(send.idPrev,id)
        assertEquals(send.idLast,idVs)
        checkState(1L)
    }

    fun checkState(version:Long){
        val objPrev = parser.parse(send.prev.substring(1)) as JSONObject
        assertEquals(objPrev["version"],version)
        val objLast = parser.parse(send.last.substring(1)) as JSONObject
        assertEquals(objLast["version"],version)
    }

    // комната добавляет команды от AI
    Test fun cmdRobot() {
        val cmderRobot = CmderStub()
        val roomRobot = Room(log, send,id,null,0, cmderRobot)
        roomRobot.cmd(id,Prm("0#cmd0"))
        roomRobot.cmd(id,Prm("1#cmd1"))
        roomRobot.cmd(id,Prm("2#robot"))

        assertEquals("robotAnswer",cmderRobot.cmds[3])
    }

    // после ошибки из-за AI комната заканчивает ход AI
    Test fun errCmdRobot() {
        val cmderRobot = CmderStub()
        val roomRobot = Room(log, send,id,null,0, cmderRobot)
        roomRobot.cmd(id,Prm("0#cmd0"))
        roomRobot.cmd(id,Prm("1#cmd1"))
        roomRobot.cmd(id,Prm("2#errRobot"))

        assertEquals(4,cmderRobot.cmds.size())
        assertEquals(Side.b,cmderRobot.sides[3])
        assertEquals("e",cmderRobot.cmds[3])
    }

    // после ошибки при исполнении команды комната создает игру заново и повторяет команды
    Test fun errCmd() {
        room.cmd(id,Prm("0#cmd0"))
        room.cmd(id,Prm("1#cmd1"))

        room.cmd(id,Prm("2#errCmd"))
        assertStateAfterReset()
    }

    // комната выбрасывает нарушение протокола при исполнении команды как есть
    Test fun violationCmd() {
        room.cmd(id,Prm("0#cmd0"))
        room.cmd(id,Prm("1#cmd1"))

        assertViolation { room.cmd(id,Prm("2#violationCmd")) }
    }

    // после ошибки при создании state комната создает игру заново и повторяет команды
    Test fun errCmdJson() {
        room.cmd(id,Prm("0#cmd0"))
        room.cmd(id,Prm("1#cmd1"))

        room.cmd(id,Prm("2#errState"))
        assertStateAfterReset()
    }

    fun assertStateAfterReset(){
        cmder.assertReseted()
        assertEquals(log.last,"error")
        assertEquals(2,cmder.cmds.size())
        assertEquals("cmd0",cmder.cmds[0])
        assertEquals("cmd1",cmder.cmds[1])
    }

    Test fun outSync() {
        room.cmd(id,Prm("0#cmd0"))
        room.cmd(id,Prm("1#cmd1"))
        room.cmd(id,Prm("0#cmd"))

        assertEquals(log.last,"outSync")
        checkState(2L)

        assertEquals(cmder.cmds.size(),2)
        assertEquals(cmder.cmds[0],"cmd0")
        assertEquals(cmder.cmds[1],"cmd1")
    }

    Test fun idWin() {

    }
}

class CmderStub : CmderGame {
    var timesReseted = 0
    val sides = ArrayList<Side>()
    val cmds = ArrayList<String>()

    override fun reset() {
        timesReseted += 1
        cmds.clear()
    }

    override fun cmd(side: Side, cmd: String) {
        sides.add(side)
        cmds.add(cmd)
        if(cmd=="errCmd") throw Err("errCmd")
        if(cmd=="violationCmd") throw Violation("violationCmd")
    }

    override fun cmdRobot(sideRobot:Side): String? {
        if(cmds.lastOrNull()=="errRobot") throw Err("errRobot")
        return if(cmds.lastOrNull()=="robot") "robotAnswer" else null
    }

    override fun land(): String {
        throw UnsupportedOperationException()
    }

    override fun state(): GameState {
        if(cmds.lastOrNull()=="errState") throw Err("errState")
        return GameState(if(cmds.lastOrNull()=="win") Side.a else null,mapOf(Side.a to JSONObject(),Side.b to JSONObject()),null)
    }

    fun assertReseted(){
        assertTrue(timesReseted == 1, "игра не сброшена")
    }
}