package unitcraft.server

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

    override fun error(ex: Throwable) {
        log("error")
    }

    override fun violation(ex: Throwable) {
        log("violation")
    }

    override fun log(event: String) {
        last = event
    }

    fun assertLast(event: String){
        assertTrue(last.startsWith(event))
    }
}

class SenderTest():Sender{
    var idLast = Id("last")
    var last = ""

    var idPrev = Id("prev")
    var prev = ""

    override fun invoke(id: Id, msg: String) {
        idPrev = idLast
        prev = last
        idLast = id
        last = msg
    }
    fun assertLast(id:Id,msg:String){
        assertTrue(idLast == id && msg == last,"send msg ${msg} to ${id}")
    }
    fun assertLastOrPrev(id:Id,msg:String){
        assertTrue(idPrev == id && msg == prev || idLast == id && msg == last,"send msg ${msg} to ${id}")
    }
}

class CreatorGameTest():CreatorGame{
    var timesCreated = 0
    var game = GameMock()

    override fun createGame(mission: Int?): () -> IGame {
        return {newGame()}
    }

    fun newGame():IGame{
        timesCreated+=1
        game = GameMock()
        return game
    }

    fun assertRecreated(){
        assertTrue(timesCreated>1,"игра пересоздана")
    }

    fun assertCreated(){
        assertTrue(timesCreated==1,"игра создана")
    }
}
