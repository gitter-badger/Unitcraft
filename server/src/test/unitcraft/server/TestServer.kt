package unitcraft.server

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import kotlin.properties.Delegates
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import org.json.simple.JSONArray
import unitcraft.inject.register

class TestServer {
    val log = LogTest()
    val users = Users()
    lateinit var wser: WserTest
    val idSsn = "idSsn"
    val idSsnVs = "idSsnVs"

    lateinit var server: Server

    init {
        register<Log>(log)
        register<CmderGame>(CmderStub())
        register(users)
        register(Bttler())
        ForInject.bttl={ server.bttl }
    }

    @Before fun before() {
        wser = WserTest()
        register<Wser>(wser)
        server = Server(false)
    }

    @Test fun regAndChangeNick() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "n")
        Thread.sleep(300)
        assertEquals("msg n", log.last, "сообщения залогировано")
        assertEquals(16, wser.msgLast.length, "размер ответа 16")
        assertEquals('n', wser.msgLast[0], "ответ начинает с n")

        val pw = wser.msgLast.substring(1)
        val id = pw.substring(0, 4)

        server.onMsg(idSsn, "l$pw 0")

        assertTrue(wser.msgPrev.startsWith("u{"), "ответ дает данные юзера")
        val user = JSONValue.parse(wser.msgPrev.substring(1)) as JSONObject
        assertEquals(id, user["id"])
        assertEquals("Player", user["nick"])
        assertEquals("0", user["balance"].toString())
        assertTrue(wser.msgLast.startsWith("g{"), "игра")

        server.onMsg(idSsn, "kNick")
        assertTrue(wser.msgLast.startsWith("u{"), "ответ дает данные юзера")
        val userN = JSONValue.parse(wser.msgLast.substring(1)) as JSONObject
        assertEquals(id, userN["id"])
        assertEquals("Nick", userN["nick"])
        assertEquals("0", userN["balance"].toString())
    }

    @Test fun notExistCmd() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "xxx")
        assertTrue(log.last.startsWith("violation"))
        wser.assertClose(idSsn)
    }

    @Test fun emptyCmd() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "")
        log.assertLast("violation")
        wser.assertClose(idSsn)
    }

    @Test fun vsRobot() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "n")
        Thread.sleep(300)
        val pw = wser.msgLast.substring(1, wser.msgLast.length)
        server.onMsg(idSsn, "l$pw 0")
        server.onMsg(idSsn, "t")
        log.assertLast("vsRobot")
        assertTrue(wser.msgLast.startsWith("g{"), "игра")
    }

    @Test fun queueAndDecline() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "p1 5")
        wser.assertLast(idSsn, "squeue")

        server.onMsg(idSsn, "d")
        wser.assertLast(idSsn, "sonline")
    }

    @Test fun queueWrongBetRange() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "p5 1")
        log.assertLast("violation")
        wser.assertClose(idSsn)
    }

    @Test fun twoQueueWithoutIntersection() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "p1 5")
        wser.assertLast(idSsn,"squeue")

        server.onOpen(idSsnVs)
        server.onMsg(idSsnVs, "p10 15")
        wser.assertLast(idSsnVs,"squeue")
    }

    private fun twoMatch(){
        server.onOpen(idSsn)
        server.onMsg(idSsn, "p1 5")

        server.onOpen(idSsnVs)
        server.onMsg(idSsnVs, "p2 6")
    }

    @Test fun twoMatchThenDecline() {
        twoMatch()
        wser.assertLastOrPrev(idSsn,"smatch")
        wser.assertLastOrPrev(idSsnVs,"smatch")

        server.onMsg(idSsn, "d")
        wser.assertLastOrPrev(idSsn,"sonline")
        wser.assertLastOrPrev(idSsnVs,"squeue")
    }

    @Test fun twoMatchThenDeclineVs() {
        twoMatch()

        server.onMsg(idSsnVs, "d")
        wser.assertLastOrPrev(idSsn,"squeue")
        wser.assertLastOrPrev(idSsnVs,"sonline")
    }

    @Test fun twoMatchThenAcceptThenDecline() {
        twoMatch()

        server.onMsg(idSsn,"y")
        wser.assertLast(idSsn,"swait")

        server.onMsg(idSsn, "d")
        wser.assertLastOrPrev(idSsn,"sonline")
        wser.assertLastOrPrev(idSsnVs,"squeue")
    }

    @Test fun twoMatchThenAcceptThenDeclineVs() {
        twoMatch()
        server.onMsg(idSsn,"y")
        server.onMsg(idSsnVs, "d")

        wser.assertLastOrPrev(idSsn,"squeue")
        wser.assertLastOrPrev(idSsnVs,"sonline")
    }


    @Test fun twoMatchThenAcceptThenAcceptVs() {
        twoMatch()
        server.onMsg(idSsn,"y")
        server.onMsg(idSsnVs,"y")

        assertTrue(log.last.startsWith("vsPlayer"))
        wser.assertLastOrPrev(idSsn,"sgame")
        wser.assertLastOrPrev(idSsnVs,"sgame")
    }

    @Test fun twoMatchThenAcceptVsThenAccept() {
        twoMatch()
        server.onMsg(idSsnVs,"y")
        server.onMsg(idSsn,"y")

        assertTrue(log.last.startsWith("vsPlayer"))
        wser.assertLastOrPrev(idSsn,"sgame")
        wser.assertLastOrPrev(idSsnVs,"sgame")
    }

    @Test fun inviteThenDecline() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "cInvt 7")
        wser.assertLast(idSsn,"sinvite")

        server.onMsg(idSsn,"d")
        wser.assertLast(idSsn,"sonline")
    }

    @Test fun twoInviteEqual() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "cdev2 7")

        server.onOpen(idSsnVs)
        server.onMsg(idSsnVs, "cdev1 7")

        assertTrue(log.last.startsWith("vsPlayer"))
        wser.assertLastOrPrev(idSsn,"sgame")
        wser.assertLastOrPrev(idSsnVs,"sgame")
    }

    @Test fun twoInviteIdNotEqual() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "cnoid 7")
        wser.assertLast(idSsn,"sinvite")

        server.onOpen(idSsnVs)
        server.onMsg(idSsnVs, "cnoid 7")
        wser.assertLast(idSsnVs,"sinvite")
    }

    @Test fun twoInviteBetNotEqual() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "cdev2 7")
        wser.assertLast(idSsn,"sinvite")

        server.onOpen(idSsnVs)
        server.onMsg(idSsnVs, "cdev1 8")
        wser.assertLast(idSsnVs, "sinvite")
    }

    @Test fun vsRobotOnGame() {
        server.onOpen(idSsn)
        server.onMsg(idSsn, "cdev2 7")
        server.onOpen(idSsnVs)
        server.onMsg(idSsnVs, "cdev1 7")
        server.onMsg(idSsn, "t")

        log.assertLast("violation")
        wser.assertClose(idSsn)
    }
}