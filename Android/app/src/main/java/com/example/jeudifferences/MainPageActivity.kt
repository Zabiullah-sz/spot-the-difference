package com.example.jeudifferences

import SocketClientHandler
import ThemePreferences
import android.app.ActionBar
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.socket.emitter.Emitter
import okhttp3.internal.notify
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class MainPageActivity : BaseActivity() {
    // lateinit var selectedColor: ColorObject
    lateinit var configurationButton: Button
    lateinit var observerButton: Button
    lateinit var classicPageButton: Button
    lateinit var limitedTimePageButton: Button
    lateinit var chatButton: Button
    lateinit var tutoButton: Button
    var isSettingSpecified = false
    lateinit var eventsButton: Button
    val waitingPlayersNbr = 0
    var isSettingDone = true
    lateinit var accountSetting: Button
    lateinit var accountStat: Button
    var canRequest = true
    var playerName = ""
    var clientSocket = SocketClientHandler
    lateinit var players: Array<String>
    lateinit var playerNbr: String
    lateinit var newHost: String
    var playerStatus = 0
    private var awaitingPlayerModal: AlertDialog? = null
    lateinit var hostName: String
    lateinit var timeArray:ArrayList<String>
    lateinit var messageArray: ArrayList<message>
    lateinit var chatAdapter: ChatGameAdapter
    lateinit var chatRecycler: RecyclerView
    val playerNames = ArrayList<String>()
    private var mediaPlayer: MediaPlayer? = null


    var gameId = ""
    var timetillStart = ""
    var modifiedImage = ""
    var originalImage = ""
    var differenceNbr = ""
    var difficulty = ""
    var gameName = ""
    lateinit var decodedStringOriginal: ByteArray
    lateinit var bitmapOriginalImage: Bitmap
    lateinit var stream: ByteArrayOutputStream
    lateinit var byteArray: ByteArray

    lateinit var decodedStringModified: ByteArray
    lateinit var bitmapModifiedImage: Bitmap
    lateinit var stream1: ByteArrayOutputStream
    lateinit var byteArray1: ByteArray
    lateinit var chatLayout: RelativeLayout
    lateinit var backgroundView: View
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var circleImageView: CircleImageView
    lateinit var userNameView:TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        val sharedPref = getSharedPreferences("MyGamePrefs", Context.MODE_PRIVATE)
        playerName = sharedPref.getString("playerName", "Guest").toString()
        setTheme()

        clientSocket.connect()
        classicPageButton = findViewById(R.id.classicModeButton)
        limitedTimePageButton = findViewById(R.id.LimitedTimeModeButton)
        configurationButton = findViewById(R.id.ConfigurationButton)
        observerButton = findViewById(R.id.ObserverButton)
        eventsButton = findViewById(R.id.setEventButton)
        chatButton = findViewById(R.id.ChatButton)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val layoutView: DrawerLayout = findViewById(R.id.draw)
        val headerView: View = navView.getHeaderView(0)
        circleImageView = headerView.findViewById(R.id.circleImageView)
        userNameView = headerView.findViewById(R.id.name)
        setImageProfile()

        toggle = ActionBarDrawerToggle(
            this,
            layoutView,
            toolbar,
            R.string.open,
            R.string.close
        )
        layoutView.addDrawerListener(toggle)
        layoutView.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24)
        supportActionBar?.setHomeButtonEnabled(true)
        toggle.setToolbarNavigationClickListener {
            if (layoutView.isDrawerOpen(GravityCompat.START)) {

                Log.d("Drawer", "Closing drawer")
                layoutView.closeDrawer(GravityCompat.START)
            } else {
                Log.d("Drawer", "Opening drawer")
                layoutView.openDrawer(GravityCompat.START)

            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.accountSetting -> {
                    val accountSettingIntent = Intent(this, AccountSettingActivity::class.java)
                    startActivity(accountSettingIntent)
                    layoutView.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.accountStat -> {
                    val accountStatIntent = Intent(this, AccountStatActivity::class.java)
                    startActivity(accountStatIntent)
                    layoutView.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.accountConfig -> {
                    val playerAccountIntent = Intent(this, PlayerAccountActivity::class.java)
                    startActivity(playerAccountIntent)
                    layoutView.closeDrawer(GravityCompat.START)
                    true

                }

                R.id.logOut -> {
                    val playerLogOut = Intent(this, PlayerLogOutActivity::class.java)
                    startActivity(playerLogOut)
                    layoutView.closeDrawer(GravityCompat.START)
                    true
                }

                else -> false
            }
        }

        ChatActivity.isInchat = false
        chatLayout = findViewById(R.id.chatLayoutFrame)
        chatLayout.visibility = View.GONE

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

        if (!ChatActivity.isInchat) {
            ChatActivity.isGlobalChat = true
            ChatActivity.isInchat = true
            clientSocket.on("new_message", onNewMessage)
        }
        //Log.i("MAINPAGEActivity, on create, player name", "$playerName")

        tutoButton = findViewById(R.id.tutorielButton)

        chatButton.setOnClickListener {



            ChatActivity.isGlobalChat = true
            ChatActivity.isInchat = true
            mediaPlayer?.stop()
            val chatIntent = Intent(this, ChatActivity::class.java)
            clientSocket.removeListener("new_message")
            startActivity(chatIntent)
        }

        observerButton.setOnClickListener {
            val observerIntent = Intent(this, ObserverActivity::class.java)
            startActivity(observerIntent)
        }

        eventsButton.setOnClickListener {
            val toCalendarIntent = Intent(this, CalendarActivity::class.java)
            startActivity(toCalendarIntent)
        }

        tutoButton.setOnClickListener {
            val tutoIntent = Intent(this, TutorielActivity::class.java)
            startActivity(tutoIntent)
        }

        configurationButton.setOnClickListener {
            val configurationIntent = Intent(this, ConfigurationActivity::class.java)
            startActivity(configurationIntent)
        }

        classicPageButton.setOnClickListener {
            val classicSelectionIntent = Intent(this, ClassicSelectionActivity::class.java)
            startActivity(classicSelectionIntent)
        }
        limitedTimePageButton.setOnClickListener {
            connectionToGame()
        }
        configurationButton.setOnClickListener {
            val classicSelectionIntent = Intent(this, ConfigurationActivity::class.java)
            startActivity(classicSelectionIntent)
        }
        playerNbr = ""
        hostName = ""
        newHost = ""

        chatRecycler = findViewById(R.id.messagesView)
        messageArray = ArrayList()
        timeArray = ArrayList()
        chatAdapter = ChatGameAdapter(this, messageArray, playerName, timeArray)
        chatRecycler.layoutManager = LinearLayoutManager(this)
        chatRecycler.adapter = chatAdapter

        clientSocket.on("chat_message", onMessage)
        clientSocket.on("response_to_play_request", limitedTimeCoopMode)
        clientSocket.on("player_status", getPlayerInfo)
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    fun setImageProfile() {
        Log.i("setimage called", "called set")
        val userId = SharedPrefUtil.getUserId(this)
        val userName = SharedPrefUtil.getUserName(this)
        userNameView.setText(userName)

        userId?.let {
            val call: Call<ResponseBody> = ApiClient.userApi.getPublicProfileImage(it)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        Log.i("call succesful ", "call succesful")
                        val imageStream = response.body()?.byteStream()
                        imageStream?.let {
                            try {
                                val bitmap = BitmapFactory.decodeStream(it)
                                Log.i("image from server", "$bitmap")

                                // Set the Bitmap to the CircleImageView
                                circleImageView.setImageBitmap(bitmap)


                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        Log.i("call fail ", "call fail")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // Gérer les échecs de l'appel
                }
            })
        }
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

                if (d.has("sender") && !d.isNull("sender")) {
                    name = d.getString("sender");
                }
                if (d.has("message") && !d.isNull("message")) {
                    message = d.getString("message");
                }
                if (d.has("time") && !d.isNull("time")) {
                    time = d.getString("time");
                }
                timeArray.add(time)
                timeArray.sortBy { timeStringToSeconds(it) }
                val objectMessage = message(message, name, time, false, false, false, false)
                messageArray.add(objectMessage)
                messageArray.sortBy { it.time?.let { it1 -> timeStringToSeconds(it1) } }
                chatAdapter.notifyDataSetChanged()
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


    fun connectionToGame() {
        if (canRequest) {
            canRequest = false
            val gson = Gson()
            var gameRequestObject = gameData(2, playerName)
            val jsonString = gson.toJson(gameRequestObject)
            clientSocket.send("request_to_play", jsonString)
        }

    }

    enum class ResponseType(val type: String) {
        Starting("0"),
        Pending("1"),
        Cancelled("2")
    }

    fun playAudio() {
        mediaPlayer?.start()
    }

    private val onNewMessage = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0] as JSONObject

                Log.i("byee", "$d")
                if (d.getString("type") == "text") {

                    var message = ""
                    var time = ""
                    var chatId = ""
                    var user = ""

                    user = d.getString("sender")
                    message = d.getString("message")
                    time = d.getString("timestamp")
                    chatId = d.getString("chatId")

                    if (user != playerName) {
                        playAudio()
                        Toast.makeText(this, "Nouveau Message!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun matchSetting(data: JSONObject) {
        this.isSettingDone = false

        // Assuming 'context' is obtained from a view or passed into the adapter
        val checkBox = CheckBox(this).apply {
            text = "Is cheating allowed?"
        }

        val input = EditText(this).apply {
            hint = "Specify the time here"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(input)
            addView(checkBox)
            // Add padding or other layout parameters if necessary
        }

        AlertDialog.Builder(this).apply {
            setView(layout)
            setPositiveButton("Confirme") { dialog, which ->

                // Get the entered time, ensuring it's within the 30-300 range
                val enteredTime = input.text.toString().toIntOrNull()
                val adjustedTime = when {
                    enteredTime == null -> 30 // Default to 30 if not a number
                    enteredTime < 30 -> 30
                    enteredTime > 300 -> 300
                    else -> enteredTime
                }

                val isCheatingAllowed = checkBox.isChecked
                // Now you can use 'adjustedTime' and 'isCheatingAllowed' to send to the server
                val gson = Gson()
                val settings = GameSettings(gameId, adjustedTime, isCheatingAllowed)

                val jsonSettings = gson.toJson(settings)
                Log.i("zazaza", settings.toString())
                Log.i("zazaza", jsonSettings.toString())
                Log.i("zazaza", "${settings}")
                clientSocket.send("send_game_settings", jsonSettings)
                isSettingDone = true
                handlePendingCase(data)
            }
            show()
        }
    }

    fun handlePendingCase(data: JSONObject) {
        chatLayout.visibility = View.VISIBLE
        val responseTime = data.getString("responseType")
        playerNbr = data.getString("playerNbr")
        gameId = data.getString("gameId")
        timetillStart = data.getString("startingIn")
        modifiedImage = data.getString("modifiedImage")
        originalImage = data.getString("originalImage")
        differenceNbr = data.getString("differenceNbr")
        difficulty = data.getString("difficulty")
        isSettingSpecified = data.getBoolean("isSettingSpecified")
        gameName = data.getString("gameName")
        var hostName = data.getString("hostName")

        awaitingPlayerModal?.dismiss()
        val remainingPlayerNbr = 4 - playerNbr.toString().toInt()
        remainingPlayerNbr.toString()
        val awaitingPlayerModalbuilder = AlertDialog.Builder(this)
        awaitingPlayerModalbuilder.setMessage("En attente de $remainingPlayerNbr autres joueurs")
        awaitingPlayerModalbuilder.setCancelable(false)
        awaitingPlayerModalbuilder.setNegativeButton("Quitter") { dialog: DialogInterface, _: Int ->
            canRequest = true
            clientSocket.send("leave_game", gameId)
            chatLayout.visibility = View.GONE
            awaitingPlayerModal?.dismiss()
            dialog.dismiss()
            dialog.cancel()
        }
        if (playerName == hostName && playerNbr >= 2.toString()) {
            awaitingPlayerModalbuilder.setPositiveButton("Commencer") { dialog: DialogInterface, _: Int ->
                val gson = Gson()
                val gameRequestObject = gameData1(2, true)
                val jsonString = gson.toJson(gameRequestObject)
                clientSocket.send("request_to_play", jsonString)
                dialog.dismiss()
                dialog.cancel()
                awaitingPlayerModal?.dismiss()
            }
        }
        awaitingPlayerModal = awaitingPlayerModalbuilder.create()
        awaitingPlayerModal?.show()
    }

    private val limitedTimeCoopMode = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONObject
                val responseTime = data.getString("responseType")
                playerNbr = data.getString("playerNbr")
                gameId = data.getString("gameId")
                timetillStart = data.getString("startingIn")
                modifiedImage = data.getString("modifiedImage")
                originalImage = data.getString("originalImage")
                differenceNbr = data.getString("differenceNbr")
                difficulty = data.getString("difficulty")
                isSettingSpecified = data.getBoolean("isSettingSpecified")
                gameName = data.getString("gameName")
                var hostName = data.getString("hostName")
                var players: JSONArray

                decodedStringOriginal =
                    android.util.Base64.decode(originalImage, android.util.Base64.DEFAULT)
                bitmapOriginalImage = BitmapFactory.decodeByteArray(
                    decodedStringOriginal,
                    0,
                    decodedStringOriginal.size
                )
                stream = ByteArrayOutputStream()
                bitmapOriginalImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                byteArray = stream.toByteArray()

                decodedStringModified =
                    android.util.Base64.decode(modifiedImage, android.util.Base64.DEFAULT)
                bitmapModifiedImage = BitmapFactory.decodeByteArray(
                    decodedStringModified,
                    0,
                    decodedStringModified.size
                )
                stream1 = ByteArrayOutputStream()
                bitmapModifiedImage.compress(Bitmap.CompressFormat.PNG, 100, stream1)
                byteArray1 = stream1.toByteArray()


                when (responseTime) {
                    ResponseType.Starting.type -> {
                        playerNames.clear()
                        awaitingPlayerModal?.dismiss()
                        players = data.getJSONArray("players")
                        for (i in 0 until players.length()) {
                            playerNames.add(players.get(i) as String)
                            //Log.i("eww", "$playerNames")
                        }

                        clientSocket.removeListener("player_status")
                        clientSocket.removeListener("response_to_play_request")
                        val gameValues = data.getJSONObject("gameValues")

                        GamePageActivity.data = gameData4(
                            playerName,
                            playerNbr,
                            gameId,
                            gameName,
                            difficulty,
                            differenceNbr,
                            byteArray,
                            byteArray1,
                            timetillStart,
                            playerNames,
                            2,
                            gameValues
                        )
                        val Intent = Intent(this, GamePageActivity::class.java)
                        startActivity(Intent)
                    }

                    ResponseType.Pending.type -> {
                        if (isSettingSpecified == false) matchSetting(data)
                        else {
                            handlePendingCase(data)
                        }
                    }
                }
            }
        }
    }

    private val getPlayerInfo = Emitter.Listener() { d ->
        if (d[0] != null) {
            runOnUiThread {
                val data = d[0] as JSONObject
               /// Log.i("unu", "$byteArray1")
                //Log.i("unuu", "$byteArray")
                Log.i("newnewnew", "$data")
                playerStatus = data.getInt("playerConnectionStatus")
                newHost = data.getString("newHost")
                val playerNbr1 = data.getString("playerNbr")

                val remainingPlayers = 4 - playerNbr1.toString().toInt()
                val awaitingPlayerModal1 = AlertDialog.Builder(this)
                awaitingPlayerModal1.setMessage("En attente de $remainingPlayers autres joueurs")
                awaitingPlayerModal1.setCancelable(false)
                awaitingPlayerModal1.setNegativeButton("Quitter") { dialog: DialogInterface, _: Int ->
                    canRequest = true
                    chatLayout.visibility = View.GONE
                    clientSocket.send("leave_game", gameId)
                    awaitingPlayerModal?.dismiss()
                    awaitingPlayerModal?.cancel()
                    dialog.dismiss()
                    dialog.cancel()
                }
                if (playerStatus == 1) {
                    awaitingPlayerModal?.dismiss()
                    hostName = newHost
                    if (playerName == newHost && playerNbr1 >= 2.toString()) {
                        awaitingPlayerModal1.setPositiveButton("Commencer") { dialog: DialogInterface, _: Int ->
                            val gson = Gson()
                            val gameRequestObject = gameData1(2, true)
                            val jsonString = gson.toJson(gameRequestObject)
                            clientSocket.send("request_to_play", jsonString)
                            dialog.dismiss()
                            awaitingPlayerModal?.dismiss()
                            dialog.dismiss()
                        }
                    }
                    awaitingPlayerModal = awaitingPlayerModal1.create()
                    awaitingPlayerModal?.show()
                }

            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        //mediaPlayer?.stop()
        clientSocket.on("new_message", onNewMessage)
    }

    override fun onDestroy() {
        super.onDestroy()
        canRequest = true
        awaitingPlayerModal?.dismiss()
        awaitingPlayerModal?.cancel()


        mediaPlayer?.stop()
        resetBackgroundToOriginalTheme()
    }
    override fun onStop() {
        super.onStop()
        clientSocket.removeListener("response_to_play_request")
        clientSocket.removeListener("player_status")
        clientSocket.removeListener("chat_message")
        clientSocket.removeListener("request_to_play")
        // Remove your listeners here
    }
}
