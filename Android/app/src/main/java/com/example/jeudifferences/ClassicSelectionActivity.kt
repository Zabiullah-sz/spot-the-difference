package com.example.jeudifferences

import SocketClientHandler
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jeudifferences.databinding.ActivitySelectionPageBinding
import com.google.gson.Gson
import io.socket.emitter.Emitter
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject


class ClassicSelectionActivity: BaseActivity() {
    private lateinit var binding: ActivitySelectionPageBinding
    private lateinit var adapter: CarouselAdapter
    var clientSocket = SocketClientHandler
    var playerName =""
    lateinit var timeArray:ArrayList<String>
    lateinit var messageArray: ArrayList<message>
    lateinit var chatAdapter: ChatGameAdapter
    lateinit var chatRecycler: RecyclerView


    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        binding = ActivitySelectionPageBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        var backToMain = findViewById<Button>(R.id.backToMain4)
        // Set the chat layout to be invisible initially
        val chatLayout = findViewById<FrameLayout>(R.id.chatLayoutFrame)
        chatLayout.visibility = View.GONE
        val sharedPref = getSharedPreferences("MyGamePrefs", Context.MODE_PRIVATE)
        playerName = sharedPref.getString("playerName", "Guest").toString()

        //playButton = findViewById(R.id.playButton)
        clientSocket.connect()

        val classicIntent = intent

        chatRecycler = findViewById(R.id.messagesView)
        messageArray = ArrayList()
        timeArray = ArrayList()
        chatAdapter = ChatGameAdapter(this, messageArray, playerName, timeArray)
        chatRecycler.layoutManager = LinearLayoutManager(this)
        chatRecycler.adapter = chatAdapter

        //Log.i("helelele", "$playerName")

        clientSocket.connect()
        clientSocket.send("all_game_cards")
        clientSocket.on("all_game_cards", onGameCards)
        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            clientSocket.removeListener("all_game_cards")
            startActivity(mainPageActivity)
        }

        showLoadingDialog()
    }

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.loading_dialog_layout, null)

        // Customize loading text if needed
        val loadingText = view.findViewById<TextView>(R.id.loadingText)
        loadingText.text = "Loading..."

        builder.setView(view)
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private val onMessage = Emitter.Listener() { args ->
        Log.i("helololol", "callleded")
        if (args[0] != null) {
            Log.i("helololol", "callleded2")
            runOnUiThread {
                Log.i("helololol", "callleded3")
                val d = args[0] as JSONObject
                var name = ""
                var message = ""
                var time = ""

                name = d.getString("sender")
                message = d.getString("message")
                time = d.getString("time")
                timeArray.add(time)
                timeArray.sortBy { timeStringToSeconds(it) }
                Log.i("helololol", "callleded4")
                val objectMessage = message(message, name, time, false, false, false, false)
                messageArray.add(objectMessage)
                Log.i("helololol", "callleded5")
                messageArray.sortBy { it.time?.let { it1 -> timeStringToSeconds(it1) } }
                Log.i("helololol", "callleded6")
                chatAdapter.notifyDataSetChanged()
                Log.i("helololol", "callleded7")
                chatRecycler.scrollToPosition(messageArray.size -1)

            }
        }
    }

    private fun timeStringToSeconds(timeString: String): Int {
        val parts = timeString.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val seconds = parts[2].toInt()
        return hours * 3600 + minutes * 60 + seconds
    }


    private val onGameCards = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONArray
                var gameDataList = arrayListOf<GameInfo>()

                for (i in 0 until data.length()) {
                    val element = data.getJSONObject(i)
                    var gameName = element.get("name").toString()

                    val soloBestTimes = Json.decodeFromString<BestTimes>(element.getJSONObject("classicSoloBestTimes").toString())
                    val multiBestTimes = Json.decodeFromString<BestTimes>(element.getJSONObject("classic1v1BestTimes").toString())

                    var gameId = element.get("id").toString()
                    var gameDifficulty = element.get("difficulty")
                    if(gameDifficulty == 0) {
                        gameDifficulty = "Facile"
                    }
                    else {
                        gameDifficulty = "Difficile"
                    }

                    val gameImage = element.get("originalImage").toString()
                    val decodedString: ByteArray = Base64.decode(gameImage, Base64.DEFAULT)
                    val bitmapImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    val Game = GameInfo(gameName, gameDifficulty, bitmapImage, gameId, soloBestTimes, multiBestTimes)
                    gameDataList.add(Game)


                }
                adapter = CarouselAdapter(gameDataList, playerName,this@ClassicSelectionActivity)
                adapter.chatRecycler = chatRecycler
                clientSocket.on("chat_message", onMessage)
                binding.apply {
                    carouselRecyclerview.adapter = adapter
                    carouselRecyclerview.setAlpha(true)
                    carouselRecyclerview.setInfinite(false)
                }
                dismissLoadingDialog()
            }
        }
    }
}
