package unitcraft.server

import unitcraft.server.Server
import unitcraft.server.Ws
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.IdentityHashMap

class ServerJavaWebSocket(val server: Server, val addr: InetSocketAddress) : WebSocketServer(addr) {
    val connToWs = IdentityHashMap<WebSocket, WsJws>()

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        println("open " + conn.getRemoteSocketAddress().getAddress().getHostAddress())
        val wsJws = WsJws(conn)
        connToWs[conn] = wsJws
        server.onOpen(wsJws)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        server.onClose(connToWs[conn])
        connToWs.remove(conn)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        server.onMsg(connToWs[conn], message)
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        println("err in conn: " + conn)
        ex?.printStackTrace()
    }
}

class WsJws(val conn: WebSocket) : Ws() {
    override fun send(msg: String) {
        conn.send(msg)
    }

    override fun close() {
        conn.close()
    }

    override val isLocal = conn.getRemoteSocketAddress().getAddress().isLoopbackAddress()
}

