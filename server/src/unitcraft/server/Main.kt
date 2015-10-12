package unitcraft.server

import unitcraft.game.registerUnitcraft
import unitcraft.inject.register
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val wser = WserJavaWebSocket(InetSocketAddress(System.getenv("OPENSHIFT_DIY_IP") ?: "localhost", System.getenv("OPENSHIFT_DIY_PORT")?.toInt() ?: 8080))

    register<Log>(LogFile())
    register<Wser>(wser)
    register(Bttler())

    val server = Server()
    register(server)
    registerUnitcraft({ server.bttl.data })

    server.start()

}
