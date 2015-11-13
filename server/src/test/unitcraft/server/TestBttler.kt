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
    val log = LogTest()
    val bttler = Bttler()

    val cmder = CmderStub()
    lateinit var bttl: Bttl

    val id = Id("0000")
    val idVs = Id("0001")

    init {
        register<Log>(log)
        register<CmderGame>(cmder)
        ForInject.bttl={ bttl }
    }

    @Before fun before() {
        bttl = Bttl(id, idVs)
    }

    // bttler отсылает состояние игры
    @Test fun sendState() {
        val chain = bttler.start(null,false)
        assertEquals(chain.list[0].first,id)
        assertEquals(chain.list[1].first,idVs)
        ensureVersion(chain.list[0],0L)
        ensureVersion(chain.list[1],0L)

        val chain2 = bttler.cmd(id, Prm("0#cmd"))

        assertEquals(chain2.list[0].first,id)
        assertEquals(chain2.list[1].first,idVs)
        ensureVersion(chain2.list[0],1L)
        ensureVersion(chain2.list[1],1L)
    }

    private fun ensureVersion(elemChain:Pair<Id, String>,version: Long) {
        val objPrev = parser.parse(elemChain.second.substring(1)) as JSONObject
        assertEquals(objPrev["version"], version)
    }

    // bttler добавляет команды от AI
    @Test fun cmdRobot() {
        bttl = Bttl(id, null)
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))
        bttler.cmd(id, Prm("2#robot"))

        assertEquals("robotAnswer", bttl.cmds[3].second)
    }

    // после ошибки из-за AI bttler заканчивает ход AI
    @Test fun errCmdRobot() {
        bttl = Bttl(id, null)
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))
        bttler.cmd(id, Prm("2#errRobot"))

        assertEquals(4, bttl.cmds.size)
        assertEquals(bttl.sideRobot(), bttl.cmds[3].first)
        assertEquals("e", bttl.cmds[3].second)
    }

    // bttler меняет сторону AI после w
    @Test fun sideChange(){
        bttl = Bttl(id, null)
        assertEquals(Side.b,bttl.sideRobot())
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#w"))
        assertEquals(Side.a,bttl.sideRobot())
    }

    private fun assertStateAfterReset() {
        cmder.assertReseted()
        assertEquals(log.last, "error")
        assertEquals(2, cmder.cmds.size)
        assertEquals("cmd0", cmder.cmds[0])
        assertEquals("cmd1", cmder.cmds[1])
    }

    // после ошибки при исполнении команды bttler создает партию заново и повторяет команды
    @Test fun errCmd() {
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))

        bttler.cmd(id, Prm("2#errCmd"))
        assertStateAfterReset()
    }

    // bttler перебрасывает нарушение протокола выше и не сбрасывает состояние
    @Test fun violationCmd() {
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))

        assertViolation { bttler.cmd(id, Prm("2#violationCmd")) }
    }


    // после ошибки при создании state bttler создает партию заново и повторяет команды
    @Test fun errCmdJson() {
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))

        bttler.cmd(id, Prm("2#errState"))
        assertStateAfterReset()
    }

    @Test fun outSync() {
        bttler.cmd(id, Prm("0#cmd0"))
        bttler.cmd(id, Prm("1#cmd1"))
        val chain = bttler.cmd(id, Prm("0#cmd"))

        assertEquals(log.last, "outSync")
        ensureVersion(chain.list[0],2L)

        assertEquals(cmder.cmds.size, 2)
        assertEquals(cmder.cmds[0], "cmd0")
        assertEquals(cmder.cmds[1], "cmd1")
    }

    @Test fun idWin() {

    }
}