package unitcraft.server

import org.junit.Test
import kotlin.test.assertEquals
import java.io.File

class TestUsers {
    Test fun regAndChangeNick() {
        val log = LogTest()
        val users = Users(log)
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

    Test fun newKey() {
        val log = LogTest()
        val users = Users(log)
        for(i in 1..100){
            val id = Users.keyToId(users.newKey())
            users.add(id,"")
        }
    }
}