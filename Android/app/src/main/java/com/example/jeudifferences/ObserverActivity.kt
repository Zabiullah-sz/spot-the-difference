package com.example.jeudifferences

import SocketClientHandler
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jeudifferences.databinding.ActivityObserverBinding
import com.jackandphantom.carouselrecyclerview.CarouselRecyclerview
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ObserverActivity: BaseActivity() {
    var activeGamesList = arrayListOf<ActiveGames>()

    var gameID = ""
    var playerNbr = ""
    var waitingPlayers = ArrayList<String>()
    var gameId = ""
    var timetillStart = ""
    var modifiedImage = ""
    var originalImage = ""
    var differenceNbr = ""
    var difficulty = ""
    var gameName = ""
    var hostName = ""
    lateinit var players: JSONArray
    val playerNames = ArrayList<String>()
    lateinit var decodedStringModified: ByteArray
    lateinit var bitmapModifiedImage: Bitmap
    lateinit var stream1: ByteArrayOutputStream
    lateinit var byteArray1: ByteArray
    lateinit var decodedStringOriginal: ByteArray
    lateinit var bitmapOriginalImage: Bitmap
    lateinit var stream: ByteArrayOutputStream
    lateinit var byteArray:ByteArray

    private lateinit var refreshButton: Button
    private lateinit var carousel: RecyclerView
    var clientSocket = SocketClientHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_observer)
        setTheme()
        refreshButton = findViewById(R.id.observerRefreshButton)
        carousel = findViewById(R.id.carouselRecyclerviewPlayerList)
        carousel.layoutManager = LinearLayoutManager(this)

        val observerCarousel = ObserverCarousel(activeGamesList, this)

        carousel.adapter = observerCarousel

        var backToMian= findViewById<Button>(R.id.backToMain2)

        backToMian.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            clientSocket.removeListener("active_games")
            clientSocket.removeListener("response_to_join_game_observer_request")
            startActivity(mainPageActivity)
        }

        refreshButton.setOnClickListener {
            clientSocket.send("active_games")
        }

        clientSocket.on("active_games", updateActiveGames)
        clientSocket.send("active_games")

        clientSocket.on("response_to_join_game_observer_request", observeGame)
    }

    private val updateActiveGames = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONArray
                Log.d("GG", data.toString())
                activeGamesList.clear()

                for (i in 0 until data.length()) {
                    val element = data.getJSONObject(i)
                    var gameId = element.get("id").toString()

                    var gameMode = element.get("gameMode")
                    when(gameMode) {
                        0 -> gameMode = "Clasique 1v1"
                        1 -> gameMode = "Classique Solo"
                        2 -> gameMode = "Temps limité Coop"
                        3 -> gameMode = "Temps limité Solo"
                    }

                    var players = element.getJSONArray("players")

                    activeGamesList.add(ActiveGames(gameMode.toString(), gameId, players.length()))
                }
                carousel.adapter?.notifyDataSetChanged()
            }
        }
    }

    private val observeGame = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONObject
                Log.d("GG", data.toString())

                playerNbr = data.getString("playerNbr")
                timetillStart = data.getString("startingIn")
                modifiedImage = data.getString("modifiedImage")
                originalImage = data.getString("originalImage")
                differenceNbr = data.getString("differenceNbr")
                difficulty = data.getString("difficulty")
                gameName = data.getString("gameName")
                hostName = data.getString("hostName")
                players = data.getJSONArray("players")
                val gameValues = data.getJSONObject("gameValues")
                for (i in 0 until players.length()) {
                    playerNames.add(players.get(i) as String)
                    //Log.i("eww", "$playerNames")
                }

                decodedStringModified = android.util.Base64.decode(
                    modifiedImage,
                    android.util.Base64.DEFAULT
                )
                bitmapModifiedImage = BitmapFactory.decodeByteArray(
                    decodedStringModified,
                    0,
                    decodedStringModified.size
                )
                stream1 = ByteArrayOutputStream()
                bitmapModifiedImage.compress(Bitmap.CompressFormat.PNG, 100, stream1)
                byteArray1 = stream1.toByteArray()

                decodedStringOriginal = android.util.Base64.decode(originalImage, android.util.Base64.DEFAULT)
                bitmapOriginalImage = BitmapFactory.decodeByteArray(decodedStringOriginal, 0, decodedStringOriginal.size)
                stream = ByteArrayOutputStream()
                bitmapOriginalImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                byteArray = stream.toByteArray()

                ObserverGamePageActivity.data = gameData4("null",playerNbr, gameId,gameName,difficulty,
                    differenceNbr, byteArray, byteArray1, timetillStart, playerNames, 0, gameValues)

                val Intent = Intent(this, ObserverGamePageActivity::class.java)
                ContextCompat.startActivity(this, Intent, null)
            }
        }
    }

}
