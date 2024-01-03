
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.net.URISyntaxException

object SocketClientHandler {
    private var clientSocket: Socket
    private var currentPlayer: String = "player 1"

    // Déclarer onUpdatePlayer comme nullable
    private var onUpdatePlayer: Emitter.Listener? = null

    init {
        val options = IO.Options()
        options.transports = arrayOf("websocket")
        options.upgrade = false

        try {

            clientSocket = IO.socket("http://10.0.0.202:2048")//("http://10.200.49.35:2048")//("http://10.0.2.2:2048") //("http://10.200.49.35:2048") //("http://10.0.2.2:2048", options)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }

        // Initialiser onUpdatePlayer ici
        onUpdatePlayer = Emitter.Listener { args ->
            if (args.isNotEmpty() && args[0] is String) {
                val updatedPlayer = args[0] as String
                setCurrentPlayer(updatedPlayer)
            }
        }

        // Ajouter le gestionnaire d'événements
        clientSocket.on("updatePlayer", onUpdatePlayer!!)
    }

    fun connect() {
        if (!isSocketAlive()) {
            clientSocket = IO.socket("http://ec2-3-99-221-92.ca-central-1.compute.amazonaws.com:2048")//("http://ec2-3-99-247-102.ca-central-1.compute.amazonaws.com:2048")//("http://10.0.2.2:2048") //("http://10.200.49.35:2048")
        }
        clientSocket.connect()
    }

    fun disconnect() {
        clientSocket.disconnect()
    }

    fun isSocketAlive(): Boolean {
        return clientSocket.connected()
    }

    fun on(event: String, listener: Emitter.Listener) {
        Log.i("socketsocketsocket", "on $event")
        clientSocket.on(event, listener)
    }

    fun send(event: String, data: Any? = null) {
        if (data != null) {
            clientSocket.emit(event, data)
        } else {
            clientSocket.emit(event)
        }
    }

    fun removeListener(event: String) {
        Log.i("socketsocketsocket", "destroy $event")
        clientSocket.off(event)
    }

    fun getCurrentPlayer(): String {
        return currentPlayer
    }

    fun setCurrentPlayer(player: String) {
        currentPlayer = player
    }
}
