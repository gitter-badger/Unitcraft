package unitcraft.server

import unitcraft.server.ServerImplJavaWebSocket
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val server = Server(LogFile())
    val addr = InetSocketAddress(System.getenv("OPENSHIFT_DIY_IP") ?: "localhost", System.getenv("OPENSHIFT_DIY_PORT")?.toInt() ?: 8080)
    ServerImplJavaWebSocket(server, addr).start()
    println("Server started at $addr")
}
