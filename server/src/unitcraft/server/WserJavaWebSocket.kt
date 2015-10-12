package unitcraft.server

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.*

class WserJavaWebSocket(val addr: InetSocketAddress) : WebSocketServer(addr), Wser {
    private var wss = HashMap<String, WebSocket>()

    private lateinit var slotOnOpen: (String, Boolean) -> Unit
    private lateinit var slotOnMsg: (String, String) -> Unit
    private lateinit var slotOnClose: (String) -> Unit

    override fun onOpen(fn: (String, Boolean) -> Unit) {
        slotOnOpen = fn
    }

    override fun onMsg(fn: (String, String) -> Unit) {
        slotOnMsg = fn
    }

    override fun onClose(fn: (String) -> Unit) {
        slotOnClose = fn
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val key = toKey(conn)
        wss[key] = conn
        slotOnOpen(key, conn.remoteSocketAddress.address.isLoopbackAddress)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        val key = toKey(conn)
        slotOnClose(key)
        wss.remove(key)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val key = toKey(conn)
        slotOnMsg(key, message)
    }

    override fun onError(ws: WebSocket, ex: Exception) {
        println("err in ws: " + toKey(ws))
        ex.printStackTrace()
    }

    override fun send(key: String, msg: String) {
        wss[key]?.send(msg)
    }

    override fun close(key: String) {
        wss[key]?.close()
    }

    private fun toKey(ws: WebSocket) = ws.hashCode().toString()
}


interface Wser {
    fun onOpen(fn: (String, Boolean) -> Unit)
    fun onMsg(fn: (String, String) -> Unit)
    fun onClose(fn: (String) -> Unit)
    fun start()
    fun send(key: String, msg: String)
    fun close(key: String)
}