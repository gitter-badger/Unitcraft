package unitcraft.server

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

interface Log {
    fun open() {
        log("open")
    }
    fun close() {
        log("close")
    }
    fun msg(msg: String) {
        log("msg $msg")
    }
    fun violation(ex: Throwable) {
        ex.printStackTrace()
        log("violation")
    }
    fun error(ex: Throwable) {
        log("error")
        ex.printStackTrace()
    }
    fun vsRobot(id: Id) {
        log("vsRobot $id")
    }
    fun vsPlayer(idA: Id, idB: Id) {
        log("vsPlayer $idA $idB")
    }
    fun end(idWin: String) {
        log("end " + idWin)
    }
    fun outSync() {
        log("outSync")
    }
    fun log(event: String)
}

class LogFile : Log {
    val frmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    val zoneId = ZoneId.of("Europe/Moscow")

    override fun log(event: String) {
        println(frmt.format(ZonedDateTime.now(zoneId)) + " " + event)
    }
}
