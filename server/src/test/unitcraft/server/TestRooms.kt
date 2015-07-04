package unitcraft.server

import org.junit.Test
import org.junit.Before
import kotlin.properties.Delegates
import kotlin.test.assertTrue

class TestRooms {
    var log: LogTest by Delegates.notNull()
    var send: SenderTest by Delegates.notNull()
    var rooms: Rooms by Delegates.notNull()
    var id: Id by Delegates.notNull()
    var idVs: Id by Delegates.notNull()

    Before fun before(){
        log = LogTest()
        send = SenderTest()
        rooms = Rooms(log, send,CreatorGameStub())
        id = Id("0000")
        idVs = Id("0001")
    }

    Test fun queueAndDecline() {
        rooms.queue(id, 1, 5)
        send.assertLast(id,"squeue")

        rooms.decline(id)
        send.assertLast(id,"sonline")
    }

    Test fun queueWrongBetRange() {
        assertViolation { rooms.queue(id, 5, 1) }
    }

    Test fun twoQueueWithoutIntersection() {
        rooms.queue(id, 1, 5)
        send.assertLast(id,"squeue")

        rooms.queue(idVs, 10, 15)
        send.assertLast(idVs,"squeue")
    }

    Test fun twoMatchThenDecline() {
        rooms.queue(id, 1, 5)
        send.assertLast(id,"squeue")

        rooms.queue(idVs, 1, 5)
        send.assertLastOrPrev(id,"smatch")
        send.assertLastOrPrev(idVs,"smatch")

        rooms.decline(id)
        send.assertLastOrPrev(id,"sonline")
        send.assertLastOrPrev(idVs,"squeue")
    }

    Test fun twoMatchThenDeclineVs() {
        rooms.queue(id, 1, 5)
        rooms.queue(idVs, 1, 5)

        rooms.decline(idVs)
        send.assertLastOrPrev(id,"squeue")
        send.assertLastOrPrev(idVs,"sonline")
    }

    Test fun twoMatchThenAcceptThenDecline() {
        rooms.queue(id, 1, 5)
        rooms.queue(idVs, 1, 5)

        rooms.accept(id)
        send.assertLast(id,"swait")

        rooms.decline(id)
        send.assertLastOrPrev(id,"sonline")
        send.assertLastOrPrev(idVs,"squeue")
    }

    Test fun twoMatchThenAcceptThenDeclineVs() {
        rooms.queue(id, 1, 5)
        rooms.queue(idVs, 1, 5)

        rooms.accept(id)
        send.assertLast(id,"swait")

        rooms.decline(idVs)
        send.assertLastOrPrev(id,"squeue")
        send.assertLastOrPrev(idVs,"sonline")
    }

    Test fun twoMatchThenAcceptVsThenDecline() {
        rooms.queue(id, 1, 5)
        rooms.queue(idVs, 1, 5)

        rooms.accept(idVs)
        send.assertLast(idVs,"swait")

        rooms.decline(id)
        send.assertLastOrPrev(id,"sonline")
        send.assertLastOrPrev(idVs,"squeue")
    }

    Test fun twoMatchThenAcceptThenAcceptVs() {
        rooms.queue(id, 1, 5)
        rooms.queue(idVs, 1, 5)
        rooms.accept(id)

        rooms.accept(idVs)
        assertTrue(log.last.startsWith("vsPlayer"))
        send.assertLastOrPrev(id,"sgame")
        send.assertLastOrPrev(idVs,"sgame")
    }

    Test fun twoMatchThenAcceptVsThenAccept() {
        rooms.queue(id, 1, 5)
        rooms.queue(idVs, 1, 5)
        rooms.accept(idVs)

        rooms.accept(id)
        assertTrue(log.last.startsWith("vsPlayer"))
        send.assertLastOrPrev(id,"sgame")
        send.assertLastOrPrev(idVs,"sgame")
    }

    Test fun inviteThenDecline() {
        rooms.invite(id, idVs, 5)
        send.assertLast(id,"sinvite")

        rooms.decline(id)
        send.assertLast(id,"sonline")
    }

    Test fun twoInvite() {
        rooms.invite(id, idVs, 5)

        rooms.invite(idVs, id, 5)
        assertTrue(log.last.startsWith("vsPlayer"))
        send.assertLastOrPrev(id,"sgame")
        send.assertLastOrPrev(idVs,"sgame")
    }

    Test fun twoInviteIdNotEqual() {
        rooms.invite(id, Id("noid"), 5)
        send.assertLast(id,"sinvite")
        rooms.invite(idVs, Id("noid"), 5)
        send.assertLast(idVs,"sinvite")
    }

    Test fun twoInviteBetNotEqual() {
        rooms.invite(id, idVs, 5)
        send.assertLast(id,"sinvite")
        rooms.invite(idVs, id, 15)
        send.assertLast(idVs, "sinvite")
    }

    Test fun vsRobot() {
        rooms.vsRobot(id, 0)
        assertTrue(log.last.startsWith("vsRobot"))
    }

    Test fun vsRobotOnWrongState() {
        rooms.invite(id, idVs, 5)
        rooms.invite(idVs, id, 5)

        assertViolation { rooms.vsRobot(id) }
    }
}