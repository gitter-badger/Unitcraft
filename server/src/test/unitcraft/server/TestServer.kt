package unitcraft.server

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import kotlin.properties.Delegates
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import org.json.simple.JSONArray

class TestServer {

    var log: LogTest by Delegates.notNull()
    var server: Server by Delegates.notNull()
    var ws: WsTest by Delegates.notNull()

    Before fun before(){
        log = LogTest()
        server = Server(log)
        ws = WsTest(null,server)
    }

    Test fun regAndChangeNick() {
        ws.onMsg("n")
        assertEquals("msg n", log.last, "сообщения залогировано")
        assertEquals('n', ws.last[0], "ответ начинает с n")
        assertEquals(16, ws.last.length(), "размер ответа 16")

        val key = ws.last.substring(1)
        val id = key.substring(0, 4)

        ws.onMsg("l$key 0")
        assertTrue(ws.prev.startsWith("u{"),"ответ дает данные юзера")
        val user = JSONValue.parse(ws.prev.substring(1)) as JSONObject
        assertEquals(id, user["id"])
        assertEquals("Player", user["nick"])
        assertEquals("0", user["balance"].toString())
        assertTrue(ws.last.startsWith("g{"), "игра")

        ws.onMsg("kNick")
        assertTrue(ws.last.startsWith("u{"),"ответ дает данные юзера")
        val userN = JSONValue.parse(ws.last.substring(1)) as JSONObject
        assertEquals(id, userN["id"])
        assertEquals("Nick", userN["nick"])
        assertEquals("0", userN["balance"].toString())
    }

    Test fun notExistCmd() {
        ws.onMsg("xxx")
        assertTrue(log.last.startsWith("violation"))
        ws.assertClose()
    }

    Test fun emptyCmd() {
        ws.onMsg("")
        log.assertLast("violation")
        ws.assertClose()
    }

    Test fun vsRobot() {
        ws.onMsg("n")
        val key = ws.last.substring(1, ws.last.length())
        ws.onMsg("l$key 0")
        ws.onMsg("t")
        log.assertLast("vsRobot")
        assertTrue(ws.last.startsWith("g{"), "игра")
    }
}

class WsTest(id: String? = null, val server: Server? = null) : Ws() {
    init {
        if (id != null) login(Id(id))
    }
    var last = ""
    var prev = ""

    var closed = false
        private set

    override fun send(msg: String) {
        prev = this.last
        this.last = msg
    }

    override fun close() {
        closed = true
    }

    fun assertClose() {
        assertTrue(closed, "соединение закрыто")
    }

    fun onMsg(msg: String) {
        server!!.onMsg(this, msg)
        Thread.sleep(500)
    }
    override val isLocal = false
}