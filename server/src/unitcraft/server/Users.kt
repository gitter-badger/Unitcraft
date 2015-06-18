package unitcraft.server

import java.util.HashMap
import java.util.Random

class Users(val log: Log) {
    val users = HashMap<Id, User>()

    fun get(id: Id): User? {
        return users[id]
    }

    fun add(id: Id, digest: String) {
        if (users.contains(id)) throw Exception("busy id: $id")
        users[id] = User(id, digest)
    }

    fun changeNick(id: Id, nick: String) {
        val user = users[id]!!
        users[id] = user.copy(nick = nick)
    }

    fun newKey(): String {
        var i = 0
        while (i < 1000000) {
            val key = genKey()
            if (!users.contains(keyToId(key))) return key
            i += 1
        }
        throw Err("cant find free id")
    }

    companion object {
        private val alphanum = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        private fun genKey(): String {
            val rnd = Random()
            return StringBuilder {
                for (i in (0..14).map{rnd.nextInt(alphanum.length())}) {
                    append(alphanum[i])
                }
            }.toString()
        }

        fun keyToId(key: String) = Id(key.substring(0, 4))
    }
}

data class Id(val id: String) {init {
    if (id.length() != 4) throw IllegalArgumentException("id length != 4: $id")
}
    override fun toString(): String {
        return id
    }
}

data class User(val id: Id, val digest: String, val nick: String = "Player", val balance: Int = 0)