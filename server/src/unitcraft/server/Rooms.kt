package unitcraft.server

import java.util.HashMap
import unitcraft.land.Land

class Rooms(val log: Log, val send: Sender, val creatorGame: CreatorGame) {
    private val rooms = HashMap<Id, Room>()
    private val plays = HashMap<Id, Play>()
    private val invites = HashMap<Id, Invite>()
    private val matches = HashMap<Id, Match>()

    fun queue(id: Id, min: Int, max: Int) {
        ensureState(id, Status.online)
        if (min > max) throw Violation("min bet($min) > max bet($max)")
        // check balance
        addPlay(Play(id, min, max))
    }

    private fun addPlay(play: Play) {
        if (plays.size() == 1) {
            val playFinded = plays.values().first()
            matches[play.id] = Match(play.id, playFinded, 1)
            matches[playFinded.id] = Match(playFinded.id, play, 1)
            plays.clear()
            sendStatus(play.id)
            sendStatus(playFinded.id)
        } else {
            plays[play.id] = play
            sendStatus(play.id)
        }
    }

    fun invite(id: Id, idVs: Id, bet: Int) {
        ensureState(id, Status.online)
        // check balance
        val invite = invites[idVs]
        if (invite != null && id == invite.idVs && bet == invite.bet) {
            vsPlayer(id, invite.id, bet)
            sendStatus(id)
            sendStatus(invite.id)
        } else {
            invites[id] = Invite(id, idVs, bet)
            sendStatus(id)
        }
    }

    fun decline(id: Id) {
        val status = status(id)
        when (status) {
            Status.queue, Status.invite, Status.match, Status.wait -> close(id)
            else -> throw Violation("decline: wrong status $status")
        }
        sendStatus(id)
    }

    fun login(id: Id, mission: Int) {
        if (rooms[id] != null)
            refresh(id)
        else
            vsRobot(id, mission)
        // TODO следует чистить комнаты тех, кто уже 5 минут как отключился
        // только если room.size()>100
    }

    fun close(id: Id) {
        val status = status(id)
        when (status) {
            Status.queue -> plays.remove(id)
            Status.invite -> invites.remove(id)
            Status.match -> {
                val match = matches[id]!!
                matches.remove(match.id)
                matches.remove(match.playVs.id)
                addPlay(match.playVs)
            }
            Status.wait -> {
                val match = matches[id]!!
                matches.remove(match.id)
                matches.remove(match.playVs.id)
                addPlay(match.playVs)
            }
        }
    }

    private fun status(id: Id): Status {
        val room = rooms[id]
        return when {
            (room != null && !room.isVsRobot && room.idWin() == null) -> Status.game
            id in matches && matches[id]!!.accepted -> Status.wait
            id in matches -> Status.match
            id in plays -> Status.queue
            id in invites -> Status.invite
            else -> Status.online
        }
    }

    private fun sendStatus(id: Id) {
        send(id, "s" + status(id))
    }

    private fun ensureState(id: Id, state: Status) {
        val actual = status(id)
        if (actual != state) throw Violation("wrong state $actual expected $state")
    }

    private fun ensureStateNotGame(id: Id) {
        val actual = status(id)
        if (actual == Status.game) throw Violation("State.game is not allowed")
    }

    fun akt(id: Id, prm: Prm) {
        val room = rooms[id]!!
        room.cmd(id, prm)
        if (room.idB != null && room.idWin() != null) {
            log.end(room.idWin().toString())
            sendStatus(room.idA)
            sendStatus(room.idB)
        }
    }

    fun vsRobot(id: Id, mission: Int? = null) {
        ensureStateNotGame(id)
        log.vsRobot(id)
        val room = Room(log, send, id, null, 0, creatorGame.createGame(mission))
        rooms[id] = room
    }

    private fun vsPlayer(idA: Id, idB: Id, bet: Int) {
        ensureStateNotGame(idA)
        ensureStateNotGame(idB)
        log.vsPlayer(idA, idB)
        val room = Room(log, send, idA, idB, bet, creatorGame.createGame())
        rooms[idA] = room
        rooms[idB] = room
    }

    fun refresh(id: Id) {
        rooms[id]!!.refresh(id)
    }

    fun land(id: Id) {
        rooms[id]!!.land(id)
    }

    fun accept(id: Id) {
        ensureState(id, Status.match)
        val match = matches[id]!!
        val matchVs = matches[match.playVs.id]!!
        if (matchVs.accepted) {
            vsPlayer(id, match.playVs.id, match.bet)
            matches.remove(match.id)
            matches.remove(match.playVs.id)
            sendStatus(match.playVs.id)
        } else {
            match.accepted = true
        }
        sendStatus(id)
    }

    companion object {
        val second = 1000000000L
    }
}

//состояние пользователя относительно очереди
//отображается в 3м квадрате в интерфейсе
private enum class Status {
    // свободен
    online,
    // выслал приглашение
    invite,
    // в очереди
    queue,
    // матч найден
    match,
    // матч найден и принят пользователем
    wait,
    // играет с человеком
    game
}

data class Play(val id: Id, val min: Int, val max: Int) {init {
    if (min < 1 || min > max) throw IllegalArgumentException("BetRange min $min max $max")
}
}

data class Invite(val id: Id, val idVs: Id, val bet: Int) {init {
    if (bet < 1) throw IllegalArgumentException("Invite bet $bet")
}
}

data class Match(val id: Id, val playVs: Play, val bet: Int, var accepted: Boolean = false)

interface CreatorGame {
    fun createGame(mission:Int?=null):CmderGame
}