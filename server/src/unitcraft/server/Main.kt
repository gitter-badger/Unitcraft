package unitcraft.server

import unitcraft.game.registerUnitcraft
import unitcraft.inject.register
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val wser = WserJavaWebSocket(
            InetSocketAddress(System.getenv("OPENSHIFT_DIY_IP") ?: "localhost",
            System.getenv("OPENSHIFT_DIY_PORT")?.toInt() ?: 8080)
    )

    register<Log>(LogFile())
    register<Wser>(wser)
    register(Bttler())
    register(Users())

    val server = Server(System.getProperty("os.name").startsWith("Windows"))
    register(server)
    ForInject.bttl={ server.bttl }
    registerUnitcraft({ server.bttl.data })

    server.start()

}

object ForInject {
    lateinit var bttl: () -> Bttl
}

fun injectBttl() = lazy(LazyThreadSafetyMode.NONE){ForInject.bttl}