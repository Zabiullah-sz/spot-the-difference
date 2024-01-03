package com.example.jeudifferences
import SocketClientHandler
import ThemePreferences
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.jeudifferences.databinding.ActivityConfigurationBinding
import com.jackandphantom.carouselrecyclerview.CarouselRecyclerview
import io.socket.emitter.Emitter
import org.json.JSONArray
import android.media.MediaPlayer
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import kotlinx.serialization.json.Json
import org.json.JSONObject

class ConfigurationActivity: BaseActivity() {
    private var mediaPlayer: MediaPlayer? = null
    var errorSound = 0
    var sucessSound = 0
    var gameDataList = arrayListOf<GameInfo>()


    lateinit var confirmButton1:Button
    lateinit var confirmButton2:Button
    lateinit var listenSucessButton:Button
    lateinit var listenErrorButton:Button
    lateinit var chatButton:Button

    private lateinit var returnButton: Button
    //private lateinit var gameResetTimeButton: ImageButton
    private lateinit var deleteGamesButton: ImageButton
    private lateinit var gameConstantButton: ImageButton
    private lateinit var carousel: CarouselRecyclerview
    private lateinit var binding: ActivityConfigurationBinding
    var clientSocket = SocketClientHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setTheme()
        clientSocket.connect()

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

        ChatActivity.isInchat = false
        if (!ChatActivity.isInchat) {
            ChatActivity.isGlobalChat = true
            clientSocket.on("new_message", onNewMessage)
        }

        returnButton = findViewById(R.id.returnButton)
        deleteGamesButton = findViewById(R.id.deleteGamesButton)
        //gameResetTimeButton = findViewById(R.id.gameResetTimeButton)
        gameConstantButton = findViewById(R.id.gameConstantButton)
        chatButton = findViewById(R.id.ChatButton3)

        changeAllButtonState(false)
        carousel = findViewById(R.id.carouselRecyclerviewPlayerList)

        var sounds = arrayOf("Son 1", "Son 2")
        val spinnerSucess = findViewById<Spinner>(R.id.spinnerSucess)
        val spinnerError = findViewById<Spinner>(R.id.spinnerError)
        confirmButton1 = findViewById(R.id.SucessSoundButton)
        confirmButton2 = findViewById(R.id.ErrorSoundButton)
        listenSucessButton = findViewById(R.id.listenSucess)
        listenErrorButton = findViewById(R.id.listenError)
      //  gameHistoryButton = findViewById(R.id.gameHistoryButton)


        val configCarousel = ConfigurationCarousel(gameDataList, this)
        binding.apply {
            carousel.adapter = configCarousel
            carousel.setAlpha(true)
            carousel.setInfinite(false)
        }

        chatButton.setOnClickListener {
            ChatActivity.isGlobalChat = true
            ChatActivity.isInchat = true
            mediaPlayer?.stop()
            val chatIntent = Intent (this, ChatActivity::class.java)
            clientSocket.removeListener("new_message")
            startActivity(chatIntent)
        }

//        gameResetTimeButton.setOnClickListener {
//            val dialog = WarnDialog(this)
//            dialog.show("réinitialiser les meilleurs temps de tous les jeux?") { clientSocket.send("reset_all_best_times") }
//        }

        deleteGamesButton.setOnClickListener {
            val dialog = WarnDialog(this)
            dialog.show("supprimer tous les jeux?") { clientSocket.send("delete_all_cards") }
        }

        returnButton.setOnClickListener {
            val classicSelectionIntent = Intent(this, MainPageActivity::class.java)
            startActivity(classicSelectionIntent)
        }

        gameConstantButton.setOnClickListener {
            val classicSelectionIntent = Intent(this, GameConstantDialog::class.java)
            startActivity(classicSelectionIntent)
        }

        listenErrorButton.setOnClickListener {
            if(errorSound == 3) {
                mediaPlayer = MediaPlayer.create(this@ConfigurationActivity, R.raw.error)
                mediaPlayer?.start()
            }
            else if(errorSound == 4) {
                mediaPlayer = MediaPlayer.create(this@ConfigurationActivity, R.raw.error1)
                mediaPlayer?.start()
            }
        }
        listenSucessButton.setOnClickListener {
            if(sucessSound == 1) {
                mediaPlayer = MediaPlayer.create(this@ConfigurationActivity, R.raw.success)
                mediaPlayer?.start()
            }
            else if (sucessSound == 2) {
                mediaPlayer = MediaPlayer.create(this@ConfigurationActivity, R.raw.success1)
                mediaPlayer?.start()
            }
        }

        confirmButton1.setOnClickListener {
            GamePageActivity.soundSucess = sucessSound
        }
        confirmButton2.setOnClickListener { GamePageActivity.soundError = errorSound }

        if (spinnerSucess != null) {
            val adapter1 = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item, sounds
            )
            spinnerSucess.adapter = adapter1
        }
        if (spinnerError != null) {
            val adapter2 = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item, sounds
            )
            spinnerError.adapter = adapter2
        }

        spinnerSucess.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position == 0) {
                    sucessSound = 1
                }
                else if(position == 1) {
                    sucessSound = 2
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        spinnerError.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position == 0) {
                    errorSound = 3
                }
                else if(position == 1) {
                    errorSound = 4
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        clientSocket.on("all_game_cards", onGameCards)
        clientSocket.on("game_card", onGameCard)
        //clientSocket.on("frontend_card_times", updateGameScore)
       // clientSocket.on("all_frontend_card_times", updateAllGameScore)
        clientSocket.send("all_game_cards")
    }

    private fun changeAllButtonState(state: Boolean) {
        returnButton.isEnabled = state
        deleteGamesButton.isEnabled = state
        //gameResetTimeButton.isEnabled = state
        gameConstantButton.isEnabled = state
        chatButton.isEnabled = state
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

    }

    private val onNewMessage = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0] as JSONObject
                Log.i("byee", "$d")
                if(d.getString("type") == "text") {
                    var message = ""
                    var time = ""
                    var chatId = ""
                    var user = ""

                    user = d.getString("sender")
                    message = d.getString("message")
                    time = d.getString("timestamp")
                    chatId = d.getString("chatId")
                    playAudio()
                    Toast.makeText(this, "Nouveau Message!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playAudio() {
        //mediaPlayer = MediaPlayer.create(this, R.raw.sound)
        mediaPlayer?.start()
    }

    private val onGameCards = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONArray
                gameDataList.clear()

                for (i in 0 until data.length()) {
                    val element = data.getJSONObject(i)
                    addGame(element)
                }
                carousel.adapter?.notifyDataSetChanged()
                changeAllButtonState(true)
            }
        }
    }

    private val onGameCard = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONObject
                addGame(data)
                carousel.adapter?.notifyItemInserted(-1)
            }
        }
    }

//    private val updateGameScore = Emitter.Listener() { args ->
//        if (args[0] != null) {
//            runOnUiThread {
//                val data = args[0] as JSONObject
//                val id = updateScore(data)
//                carousel.adapter?.notifyItemChanged(id)
//            }
//        }
//    }

//    private val updateAllGameScore = Emitter.Listener() { args ->
//        if (args[0] != null) {
//            runOnUiThread {
//                val data = args[0] as JSONArray
//
////                for (i in 0 until data.length()) {
////                    val element = data.getJSONObject(i)
////                    val id = updateScore(element)
////                    carousel.adapter?.notifyItemChanged(id)
////                }
//
//            }
//        }
//    }

//    private fun updateScore(data: JSONObject): Int {
//        val gameId = data.get("id").toString()
//
//        val game: GameInfo? = gameDataList.find { x -> x.gameId == gameId }
//       // if(game?.soloBestTimes != null)
//        game?.soloBestTimes = Json.decodeFromString<BestTimes>(data.getJSONObject("classicSoloBestTimes").toString())
//        game?.multiBestTimes = Json.decodeFromString<BestTimes>(data.getJSONObject("classic1v1BestTimes").toString())
//
//        return gameDataList.indexOf(game)
//    }
    private fun addGame(data: JSONObject) {
        var gameName = data.get("name").toString()

        val soloBestTimes = Json.decodeFromString<BestTimes>(data.getJSONObject("classicSoloBestTimes").toString())
        val multiBestTimes = Json.decodeFromString<BestTimes>(data.getJSONObject("classic1v1BestTimes").toString())

        var gameDifficulty = data.get("difficulty")
        if(gameDifficulty == 0) {
            gameDifficulty = "Facile"
        }
        else {
            gameDifficulty = "Difficile"
        }
        var gameId = data.get("id").toString()

        val gameImage = data.get("originalImage").toString()
        val decodedString: ByteArray = Base64.decode(gameImage, Base64.DEFAULT)
        val bitmapImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        gameDataList.add(GameInfo(gameName, gameDifficulty, bitmapImage, gameId, soloBestTimes, multiBestTimes))
    }

    override fun onRestart() {
        super.onRestart()
        //mediaPlayer?.stop()
        clientSocket.on("new_message", onNewMessage)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
    }
}
