package unitcraft.server

import org.junit.Before
import org.junit.Test
import unitcraft.inject.register
import kotlin.test.assertEquals
import java.io.File

class TestUsers {
    val log = LogTest()
    lateinit var users:Users

    init{
        register<Log>(log)
    }

    @Before fun before(){
        users = Users()
    }

    @Test fun regAndChangeNick() {
        val id = Id("0000")
        users.add(id,"")

        val user = users[id]!!
        assertEquals(id, user.id)
        assertEquals("Player", user.nick)
        assertEquals(0, user.balance)

        users.changeNick(id,"Nick")
        val user2 = users[id]!!
        assertEquals("Nick", user2.nick)
    }

    @Test fun newKey() {
        repeat(100){
            val id = Users.keyToId(users.newKey())
            users.add(id,"")
        }
    }
}