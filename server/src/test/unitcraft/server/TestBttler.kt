package unitcraft.server

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.Before
import org.junit.Test
import unitcraft.inject.inject
import unitcraft.inject.register
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TestBttler {
    val parser = JSONParser()
    var log = LogTest()
    //var wser = SenderTest()
    //var game: CmderStub  by inject()
    var bttler = Bttler()

    val cmder = CmderStub()
    lateinit var bttl: Bttl

    val id = Id("0000")
    val idVs = Id("0001")

    init {
        register<Log>(log)
        register<CmderGame>(cmder)
        register({ bttl })
    }

    @Before fun before() {
        bttl = Bttl(id, idVs)
    }

    // комната отсылает состояние игры
    @Test fun sendState() {
        val chain = bttler.start(null,false)
        assertEquals(chain.list[0].first,id)
        assertEquals(chain.list[1].first,idVs)
        ensureVersion(chain,0L)

        val chain2 = bttler.cmd(id, Prm("0#cmd"))

        assertEquals(chain.list[0].first,id)
        assertEquals(chain.list[1].first,idVs)
        ensureVersion(chain2,1L)
    }

    private fun ensureVersion(chain:Chain,version: Long) {
        val objPrev = parser.parse(chain.list[0].second.substring(1)) as JSONObject
        assertEquals(objPrev["version"], version)
        val objLast = parser.parse(chain.list[1].second.substring(1)) as JSONObject
        assertEquals(objLast["version"], version)
    }

    // комната добавляет команды от AI
    @Test fun cmdRobot() {
        bttl = Bttl(id, null)
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))
        bttler.cmd(id, Prm("2#robot"))

        assertEquals("robotAnswer", bttl.cmds[3])
    }

    // после ошибки из-за AI комната заканчивает ход AI
    @Test fun errCmdRobot() {
        val cmderRobot = CmderStub()
        //val roomRobot = Bttl(log, wser, idSsn, null, 0, cmderRobot)
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))
        bttler.cmd(id, Prm("2#errRobot"))

        assertEquals(4, cmderRobot.cmds.size)
        assertEquals(Side.b, cmderRobot.sides[3])
        assertEquals("e", cmderRobot.cmds[3])
    }
/*
    // после ошибки при исполнении команды комната создает игру заново и повторяет команды
    @Test fun errCmd() {
        room.cmd(idSsn, Prm("0#cmd0"))
        room.cmd(idSsn, Prm("1#cmd1"))

        room.cmd(idSsn, Prm("2#errCmd"))
        assertStateAfterReset()
    }

    // комната выбрасывает нарушение протокола при исполнении команды как есть
    @Test fun violationCmd() {
        room.cmd(idSsn, Prm("0#cmd0"))
        room.cmd(idSsn, Prm("1#cmd1"))

        assertViolation { room.cmd(idSsn, Prm("2#violationCmd")) }
    }

    // после ошибки при создании state комната создает игру заново и повторяет команды
    @Test fun errCmdJson() {
        room.cmd(idSsn, Prm("0#cmd0"))
        room.cmd(idSsn, Prm("1#cmd1"))

        room.cmd(idSsn, Prm("2#errState"))
        assertStateAfterReset()
    }

    fun assertStateAfterReset() {
        cmder.assertReseted()
        assertEquals(log.last, "error")
        assertEquals(2, cmder.cmds.size())
        assertEquals("cmd0", cmder.cmds[0])
        assertEquals("cmd1", cmder.cmds[1])
    }

    @Test fun outSync() {
        room.cmd(idSsn, Prm("0#cmd0"))
        room.cmd(idSsn, Prm("1#cmd1"))
        room.cmd(idSsn, Prm("0#cmd"))

        assertEquals(log.last, "outSync")
        ensureVersion(2L)

        assertEquals(cmder.cmds.size(), 2)
        assertEquals(cmder.cmds[0], "cmd0")
        assertEquals(cmder.cmds[1], "cmd1")
    }*/

    @Test fun idWin() {

    }
}