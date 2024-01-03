package com.example.jeudifferences
import SocketClientHandler
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import com.google.gson.Gson
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.socket.emitter.Emitter
import org.json.JSONObject
import android.util.Base64
import android.view.Gravity
import android.view.KeyEvent
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

import java.util.Timer;
import java.util.TimerTask;



import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.*

// Then use GlobalScope.launch { ... } instead of lifecycleScope.launch { ... }

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.json.JSONArray
import org.json.JSONException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ObserverGamePageActivity : BaseActivity()  {
    val clientSocket = SocketClientHandler
    var gamePlayerScore: MutableMap<String, Int> = mutableMapOf()
    private lateinit var originalImageView: ImageView
    private lateinit var modifiedImageView: ImageView
    private var volumeUpPressed = false
    private var volumeDownPressed = false
    lateinit var gameModeText:TextView
    lateinit var gameNameText:TextView
    lateinit var totalDifferences:TextView
    lateinit var nbrObservers: TextView
    var observers = 0
    var playerLeftinGame = 0

    lateinit var originalFrame: FrameLayout
    lateinit var modifiedFrame: FrameLayout
    var newScore =0
    var endGame= false

    //timer of replay
    var serverTime:Int = 0
    var timer = data.gameValues.getInt("timerTime")
    var isCheatingAllowed = data.gameValues.getBoolean("isCheatAllowed")
    private var isReplayPaused = false
    private var remainingTime: Long = 0
    private var replaySpeedFactor: Int = 1

    private var mediaPlayer: MediaPlayer? = null

    var playerActionsFalse = ArrayList<PlayerClickFalse>()
    var playerActionsValid = ArrayList<PlayerClickValid>()
    var gameMessage = ArrayList<inGameMessage>()


    //////////////////////// Replay //////////////////////////////////////////////
    var currentIndexFalse = 0
    var currentIndexValid = 0
    var currentIndexMessages = 0


    //MUTE
    val mutedPlayers = mutableSetOf<String>()
    private fun updateMutedPlayers(isChecked: Boolean, playerName: String) {


        if (isChecked) {
            // If the switch is checked, add the player's name to the muted players list
            if (data.playerNames.contains(playerName) && !mutedPlayers.contains(playerName)) {
                mutedPlayers.add(playerName)
            }
        } else {
            // If the switch is unchecked, remove the player's name from the muted players list
            mutedPlayers.remove(playerName)
        }

        // Print the list of muted players for testing purposes
        println("Muted Players: $mutedPlayers")
        Log.d("MonTag", "Muted Players: $mutedPlayers")
    }


    var cheaterName = ""
    private var gameDataList = mutableListOf<CardFiles>()
    val nextCardFiles = mutableListOf<CardFiles>()
    private var pendingCardUpdate = false
    private var isCheating = false
    var x: Float = 0.0f
    var y : Float= 0.0f
    var modified  = false
    var isNextCard = false
    var currentTime:Long=0

    private var myJob = Job()
    private var myScope = CoroutineScope(Dispatchers.Main + myJob)

    lateinit var chatRecycler: RecyclerView
    lateinit var sendBtn: ImageButton
    lateinit var messageBox: EditText
    lateinit var chatAdapter: ChatGameAdapter
    lateinit var messageArray: ArrayList<message>
    lateinit var timeArray:ArrayList<String>
    var winner: String = ""
    var isWinnerName:Boolean = false
    var playerNameServer:String=""
    var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
       // Log.i("zazazazattt", "GamePageCreated")
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.activity_classic_coop)
        //  val themePreferences = ThemePreferences(this)
        //val currentTheme = themePreferences.getSelectedTheme()
        // window.decorView.setBackgroundResource(currentTheme)
        ChatActivity.isGlobalChat =false

        clientSocket.send("is_playing")
        val playerSwitch1: Switch = findViewById(R.id.playerSwitch1)
        val playerSwitch2: Switch = findViewById(R.id.playerSwitch2)
        val playerSwitch3: Switch = findViewById(R.id.playerSwitch3)
        val playerSwitch4: Switch = findViewById(R.id.playerSwitch4)
        playerSwitch1.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            updateMutedPlayers(isChecked, data.playerNames[0])
            Log.d("MonTag", "PlayerName[0] : ${data.playerNames[0]}")

        }
        playerSwitch2.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            updateMutedPlayers(isChecked,data.playerNames[1])
        }
        playerSwitch3.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            updateMutedPlayers(isChecked, data.playerNames[2])
        }
        playerSwitch4.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            updateMutedPlayers(isChecked, data.playerNames[3])
        }

        gamePlayerScore = data.playerNames.associateWith { 0 }.toMutableMap()
        initAndUpdateScores()
        originalImageView = findViewById(R.id.originalImage)
        modifiedImageView = findViewById(R.id.modifiedImage)
        var Intent = intent
        val modified = data.modifiedImage
        val original = data.originalImage
        val gameMode = data.gameMode
        nbrObservers = findViewById(R.id.nbObservers)
        nbrObservers.visibility = View.INVISIBLE
        playerLeftinGame = data.playerNbr.toInt()

        chatRecycler = findViewById(R.id.messagesView)
        messageBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendMessageBtn)
        messageArray = ArrayList()
        timeArray = ArrayList()
        chatAdapter = ChatGameAdapter(this, messageArray, data.playerName, timeArray)
        chatRecycler.layoutManager = LinearLayoutManager(this)
        chatRecycler.adapter = chatAdapter
        gameModeText = findViewById(R.id.gameMode)
        gameNameText = findViewById(R.id.gameName)
        totalDifferences = findViewById(R.id.NbrDifference)

        clientSocket.on("chat_message", onMessage)
        clientSocket.on("global_message", onGlobalMessage)
        clientSocket.on("deserter", onDeserterMessage)
        clientSocket.on("cheat_alert", onCheatingMessage)
        //Log.i("uju", "$timer")


        sendBtn.setOnClickListener {
            sendMessage()
        }

        messageBox.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                sendMessage()
                return@OnKeyListener true
            }
            false
        })

        // Get reference to the ImageView from the layout

        originalFrame = findViewById(R.id.originalFrame)
        modifiedFrame = findViewById(R.id.modifiedFrame)

        val originalBitmap: Bitmap = BitmapFactory.decodeByteArray(original, 0, original.size)
        val modifiedBitmap: Bitmap = BitmapFactory.decodeByteArray(modified, 0, modified.size)

        // Set the image to the ImageView
        originalImageView.setImageBitmap(originalBitmap)
        modifiedImageView.setImageBitmap(modifiedBitmap)
        gameNameText.text = data.gameName
        if(gameModeObserver == 0) {
            gameModeText.text = "Classique 1V1"
        }
        if(gameModeObserver == 2) {
            gameModeText.text = "Temps limité Coop"
        }
        totalDifferences.text = "Total de différences: " + data.differenceNbr

        val click = ClickInfo()
        click.gameId = data.gameId

//        originalImageView.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
////                val x = (event.x-1.9) * (637.0/798)
////                val y = (event.y - 2) * (479.0/597)
//                x = event.x
//                y = event.y
//                click.x = x.toInt()
//                click.y = y.toInt()
//                Log.d("kbb", "Touched at X=$x, Y=$y on original image")
//                val gson = Gson()
//                val jsonString = gson.toJson(click)
//                this@GamePageActivity.modified = false
//                clientSocket.send("send_click", jsonString)
//                true
//            } else {
//                false
//            }
//        }
//
//        modifiedImageView.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
////                val x = (event.x-0.9) * (637.0/798)
////                val y = (event.y - 2) * (479.0/597)
//                x = event.x
//                y = event.y
//                click.x = x.toInt()
//                click.y = y.toInt()
//                Log.d("kbmm", "Touched at X=${x}, Y=${y} on modified image")
//                val gson = Gson()
//                val jsonString = gson.toJson(click)
//                this@GamePageActivity.modified = true
//                clientSocket.send("send_click", jsonString)
//                true
//            } else {
//                false
//            }
//        }

        val abandonButton: Button = findViewById(R.id.abandon)
        abandonButton.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.confirmation))
                setMessage(getString(R.string.do_you_want_to_quit)) // Set the message

                setPositiveButton(getString(R.string.yes)) { dialog, which ->
                    leaveGame() // Call your leaveGame function
                }

                // Add a "No" button
                setNegativeButton(getString(R.string.no)) { dialog, which ->
                    dialog.dismiss() // Dismiss the dialog
                }

                // Show the AlertDialog
                create().show()
            }
        }
        clientSocket.on("click_personal", processClickResponse)
        clientSocket.on("click_enemy", processClickResponse)
        clientSocket.on("next_card", processNextCardEvent)
        clientSocket.on("cheat", cheatFlashingOverlay)
        clientSocket.send("get_game_values")
        //clientSocket.on("cheat_alert", triggerVoteKickAlert)
        //clientSocket.on("back_to_lobby", kickOutCheater)
        clientSocket.on("player_status", processToLimitedTimeSinglePlayer)
        //clientSocket.on("game_values", processGetTime)
        clientSocket.on("endgame",processEndGame )
        clientSocket.on("time", processGetTime)

        //Log.i("gameModeObserver", "$gameModeObserver")
    }

    /////////////////////replay//////////////////////
    private fun createCountDownTimer(initialTime: Long): CountDownTimer {
        return object : CountDownTimer(initialTime, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                updateUI(remainingSeconds)

                // Check if there are actions or messages at the current time
                checkAndDisplayActionsOrMessages(remainingSeconds.toInt())
                remainingTime = millisUntilFinished

                displayErrorMessageIfTimeMatches(playerActionsFalse, remainingSeconds.toInt())
                displayValidMessageIfTimeMatches(playerActionsValid, remainingSeconds.toInt())
                displayTextMessages(gameMessage, remainingSeconds.toInt())
            }

            override fun onFinish() {
                // Your onFinish logic here
                finishReplay()
            }
        }
    }



    private val onGlobalMessage = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0]
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formattedTime = current.format(formatter)
                //Log.i("noooo", "$d")

                timeArray.add(formattedTime)
                val objectMessage = message(d as String, d,  true, false, true, true)
                messageArray.add(objectMessage)
                chatAdapter.notifyDataSetChanged()
                chatRecycler.scrollToPosition(messageArray.size -1)

                //replay
                gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))

                playerLeftinGame--
                if(gameModeObserver == 2 && playerLeftinGame < 2) {
                    handleGameEnd()
                }
            }
        }
    }

    private val onCheatingMessage = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0]
                //Log.i("messagee", "$d")
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formattedTime = current.format(formatter)

                timeArray.add(formattedTime)
                val objectMessage = message(d as String, d,  true, false, true)
                messageArray.add(objectMessage)
                chatAdapter.notifyDataSetChanged()
                chatRecycler.scrollToPosition(messageArray.size -1)
                // added new line here
                gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
            }
        }
    }

    private val processEndGame = Emitter.Listener { args ->
        //Log.i("allo after if", "bonjour before")
        if (args[0] != null) {
           // Log.i("allo befor if", "bonjour if")
            var playerName =""
            var isWinner = false
            var winnerName = ""
            runOnUiThread {
                try {
                    val dataArray = args[0] as JSONObject
                    val finalTime = dataArray.getLong("finalTime")
                    //Log.i("final time", "$finalTime")
                    val players = dataArray.getJSONArray("players")
                    //Log.i("liste des player", "$players")
                    val currentUserName = data.playerName
                    for (j in 0 until players.length()) {
                        val player = players.getJSONObject(j)
                        playerName = player.getString("name")
                        val winnerPlayer = player.getString("winner").toBoolean()
                        playerNameServer= playerName
                        if(player.getString("winner").toBoolean()) {
                            winnerName = player.getString("name")
                        }
                        isWinner =((playerName == currentUserName )&& winnerPlayer)

                    }

                    if(gameModeObserver == 2) {
                        handleGameEnd()
                    }

                    if (gameModeObserver == 0) {
                        if(isWinner) {
                            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                            val customView = inflater.inflate(R.layout.activity_congratmessage_classic, null)

                            val nameText = customView.findViewById<TextView>(R.id.winnerName)
                            nameText.text = data.playerName +  " a gagné"
                            val closeButton = customView.findViewById<Button>(R.id.closeButton)
                            val replayButton = customView.findViewById<Button>(R.id.replay)

                            val builder = AlertDialog.Builder(this)
                            builder.setView(customView)
                            builder.setCancelable(false)
                            val dialog = builder.create()
                            dialog.show()

                            closeButton.setOnClickListener {
                                dialog.dismiss()
                                clientSocket.send("leave_game", data.gameId)
                                clientSocket.removeListener("click_personal")
                                clientSocket.removeListener("click_enemy")
                                clientSocket.removeListener("next_card")
                                clientSocket.removeListener("cheat")
                                clientSocket.removeListener("get_game_values")
                                clientSocket.removeListener("game_values")
                                clientSocket.removeListener("chat_message")
                                clientSocket.removeListener("deserter")
                                clientSocket.removeListener("cheat_alert")
                                val mainActivityIntent = Intent(this, MainPageActivity::class.java)
                                startActivity(mainActivityIntent)
                            }

                            replayButton.setOnClickListener {
                                dialog.dismiss()
                                clientSocket.removeListener("time")
                                clientSocket.removeListener("click_personal")
                                clientSocket.removeListener("click_enemy")
                                clientSocket.removeListener("next_card")
                                clientSocket.removeListener("cheat")
                                clientSocket.removeListener("get_game_values")
                                clientSocket.removeListener("game_values")
                                clientSocket.removeListener("chat_message")
                                clientSocket.removeListener("deserter")
                                clientSocket.removeListener("cheat_alert")
                                //Log.i("playerActionss", "$playerActionsFalse")
                                replayGame()

                            }
                        }

                        if (!isWinner) {
                            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                            val customView = inflater.inflate(R.layout.activity_congratmessage_looser, null)

                            val nameText = customView.findViewById<TextView>(R.id.winnerName)
                            nameText.text = winnerName + " a gagné"
                            val closeButton = customView.findViewById<Button>(R.id.closeButton)
                            val replayButton = customView.findViewById<Button>(R.id.replay)
                            val builder = AlertDialog.Builder(this)
                            builder.setView(customView)
                            builder.setCancelable(false)
                            val dialog = builder.create()
                            dialog.show()

                            closeButton.setOnClickListener {
                                dialog.dismiss()
                                clientSocket.send("leave_game", data.gameId)
                                clientSocket.removeListener("click_personal")
                                clientSocket.removeListener("click_enemy")
                                clientSocket.removeListener("next_card")
                                clientSocket.removeListener("cheat")
                                clientSocket.removeListener("get_game_values")
                                clientSocket.removeListener("game_values")
                                clientSocket.removeListener("chat_message")
                                clientSocket.removeListener("deserter")
                                clientSocket.removeListener("cheat_alert")
                                val mainActivityIntent = Intent(this, MainPageActivity::class.java)
                                startActivity(mainActivityIntent)
                            }

                            replayButton.setOnClickListener {
                                dialog.dismiss()
                                clientSocket.removeListener("time")
                                clientSocket.removeListener("click_personal")
                                clientSocket.removeListener("click_enemy")
                                clientSocket.removeListener("next_card")
                                clientSocket.removeListener("cheat")
                                clientSocket.removeListener("get_game_values")
                                clientSocket.removeListener("game_values")
                                clientSocket.removeListener("chat_message")
                                clientSocket.removeListener("deserter")
                                clientSocket.removeListener("cheat_alert")
                                //Log.i("playerActionss", "$playerActionsFalse")
                                replayGame()
                            }

                        }
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val onDeserterMessage = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                val db = args[0].toString()
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formattedTime = current.format(formatter)

                timeArray.add(formattedTime)
                val objectMessage = message(db, db, true, true, false)
                messageArray.add(objectMessage)
                chatAdapter.notifyDataSetChanged()
                chatRecycler.scrollToPosition(messageArray.size -1)


                //replay
                gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
            }
        }
    }

    private val onMessage = Emitter.Listener { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0] as JSONObject
                var name = ""
                var message = ""
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formattedTime = current.format(formatter)

                name = d.getString("sender")
                message = d.getString("message")
                if (!mutedPlayers.contains(name)) {
                    timeArray.add(formattedTime)
                    val objectMessage = message(message, name, false, false, false, false)
                    messageArray.add(objectMessage)
                    chatAdapter.notifyDataSetChanged()
                    chatRecycler.scrollToPosition(messageArray.size -1)

                    //replay
                    gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
                }
            }
        }
    }

    fun sendMessage() {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = current.format(formatter)
        var message = messageBox.text.toString().trim()
        if(message.isNotEmpty() && !mutedPlayers.contains(data.playerName)) {
            timeArray.add(formattedTime)
            val sentMessage = message(message, data.playerName, false, false, false, false)
            messageArray.add(sentMessage)
            val gson = Gson()
            var messageObject = MessageData(message, data.gameId, formattedTime)
            val jsonString = gson.toJson(messageObject)
            clientSocket.send("send_chat_message", jsonString)

            //replay
            gameMessage.add(inGameMessage(sentMessage, serverTime.toLong(), formattedTime))
            chatAdapter.notifyDataSetChanged()
            messageBox.text.clear()
            chatRecycler.scrollToPosition(messageArray.size -1)
        }
        else if (mutedPlayers.contains(data.playerName)) {

            println("Vous ne pouvez pas envoyer de message, vous êtes muet.")

        }

    }

    //** on functions **//

    private val processToLimitedTimeSinglePlayer = Emitter.Listener { args ->
        //Log.i("zazazazattt", "called")
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONObject
                //Log.i("zazazazattt", "$data")
                val userName = data.getJSONObject("user").getString("name")

                if (false) {
//                    if (playerConnectionStatus == PlayerConnectionStatus.Left.ordinal) {
//                        isCoopGame = false
//                        isMultiplayer = false
//                        nbOfPlayers = 1
//                        gameMode = GameMode.LimitedTimeSolo
//                    }
                } else {
                    gamePlayerScore.remove(userName)
                    // data.playerNames.remove(userName)
                    //Log.i("zazazazattt", "$gamePlayerScore")
                    initAndUpdateScores()
                }
            }
        }
    }

    private val kickOutCheater = Emitter.Listener {args ->
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Expulsion")
                .setMessage("Vous avez été expulsé de la partie à cause de tricherie.")
                .setPositiveButton("OK") { dialog, which ->
                    // Proceed to main page
                    val kickingOut = Intent(this, MainPageActivity::class.java)
                    // Optionally add flags if needed
                    // kickingOut.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(kickingOut)
                }
                .setCancelable(false)
                .show()
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpPressed = true
                checkForBothVolumeButtons()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownPressed = true
                checkForBothVolumeButtons()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> volumeUpPressed = false
            KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownPressed = false
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun checkForBothVolumeButtons() {
        if (isCheatingAllowed) {
            if (volumeUpPressed && volumeDownPressed) {
                handleCheating()
            }
        }
    }
    private fun handleGameEnd() {
        if(gameModeObserver==2) {
            val finalScore = calculateFinalScore()
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val customView = inflater.inflate(R.layout.activity_congrat_message_limitedtime, null)


            val builder = AlertDialog.Builder(this)
            builder.setView(customView)
            builder.setCancelable(false)
            val dialog = builder.create()
            dialog.show()

            val errorMessageText = customView.findViewById<TextView>(R.id.differenceFound)
            errorMessageText.text = finalScore.toString()
            val closeButton = customView.findViewById<Button>(R.id.closeButton)

            closeButton.setOnClickListener {
                dialog.dismiss() // Close when the button is clicked
                clientSocket.send("leave_game", data.gameId)
                clientSocket.removeListener("click_personal")
                clientSocket.removeListener("click_enemy")
                clientSocket.removeListener("next_card")
                clientSocket.removeListener("cheat")
                clientSocket.removeListener("get_game_values")
                clientSocket.removeListener("game_values")
                clientSocket.removeListener("chat_message")
                clientSocket.removeListener("deserter")
                clientSocket.removeListener("cheat_alert")
                val mainActivityIntent = Intent(this, MainPageActivity::class.java)
                startActivity(mainActivityIntent)
            }
        }
    }


    ///////////////////////////////////// replay ///////////////////////////////////
    fun replayGame() {
        countDownTimer = createCountDownTimer((timer * 1000).toLong())
        countDownTimer!!.start()
        chatAdapter.clearMessages()

        //blocking chat
        sendBtn.visibility  = View.INVISIBLE
        messageBox.isClickable = false
        messageBox.isFocusable = false
        messageBox.isFocusableInTouchMode = false

        val nameTextViews = listOf(
            findViewById<TextView>(R.id.playerName1),
            findViewById<TextView>(R.id.playerName2),
            findViewById<TextView>(R.id.playerName3),
            findViewById<TextView>(R.id.playerName4)
        )

        val scoreTextViews = listOf(
            findViewById<TextView>(R.id.playerScore1),
            findViewById<TextView>(R.id.playerScore2),
            findViewById<TextView>(R.id.playerScore3),
            findViewById<TextView>(R.id.playerScore4)
        )

        for (i in nameTextViews.indices) {
            if (i < data.playerNames.size) {
                scoreTextViews[i].text = 0.toString()
            }
        }

        gamePlayerScore.clear()

        val restartButton = findViewById<ImageButton>(R.id.restartReplay)
        val stopButton = findViewById<ImageButton>(R.id.stopReplay)
        val resumeButton = findViewById<ImageButton>(R.id.resumeReplay)
        val quitButton = findViewById<Button>(R.id.leaveReplay)
        val abandonButton = findViewById<Button>(R.id.abandon)
        val reprise = findViewById<TextView>(R.id.modeReprise)
        val speed1Button= findViewById<Button>(R.id.speed1)
        val speed2Button= findViewById<Button>(R.id.speed2)
        val speed3Button= findViewById<Button>(R.id.speed3)

        restartButton.visibility = View.VISIBLE
        stopButton.visibility = View.VISIBLE
        resumeButton.visibility = View.VISIBLE
        quitButton.visibility = View.VISIBLE
        reprise.visibility = View.VISIBLE
        speed1Button.visibility = View.VISIBLE
        speed2Button.visibility = View.VISIBLE
        speed3Button.visibility = View.VISIBLE
        abandonButton.visibility = View.INVISIBLE


        restartButton.setOnClickListener { restartReplay() }
        stopButton.setOnClickListener { pauseReplay() }
        resumeButton.setOnClickListener { resumeReplay() }
        quitButton.setOnClickListener { leaveGame() }
        speed1Button.setOnClickListener { replaySpeedFactor = 1 }
        speed2Button.setOnClickListener { replaySpeedFactor = 2 }
        speed3Button.setOnClickListener { replaySpeedFactor = 3 }

    }

    fun resumeReplay() {
        if(isReplayPaused) {
            countDownTimer = createCountDownTimer(remainingTime)
            countDownTimer!!.start()
            isReplayPaused = false
        }
    }


    private fun pauseReplay() {
        countDownTimer?.cancel()
        isReplayPaused = true
    }

    private fun restartReplay() {
        // Reset variables or perform any other necessary actions
        if(isReplayPaused) {
            countDownTimer?.cancel()
            isReplayPaused = false
        }
        currentIndexFalse = 0
        currentIndexValid = 0
        currentIndexMessages = 0

        // Restart the countdown timer
        countDownTimer?.cancel() // Cancel the existing timer
        replayGame()
    }

    private val triggerVoteKickAlert = Emitter.Listener {args ->
        if (args[0] != null) {
            val name = args[0] as String
            // This will run on a non-UI thread, so you need to switch to the UI thread

            runOnUiThread {
                if(cheaterName !== name) {
                    cheaterName = name
                    showVoteKickDialog(name)
                }
            }
        }
    }
    private fun showVoteKickDialog(name: String) {
        //Log.i("zazazazazagameidd", "${data.gameId}")
        AlertDialog.Builder(this)
            .setTitle("Cheat Alert")
            .setMessage("$name triche! Votez-vous pour l'expulser?")
            .setPositiveButton("Oui") { _, _ ->
                // User voted yes, send response to server
                val gson = Gson()
                val gameRequestObject = CheaterInfo(data.gameId, name, "yes", data.playerName)
                val cheaterInfo = gson.toJson(gameRequestObject)
                clientSocket.send("kick_out_player", cheaterInfo)
            }
            .setNegativeButton("Non") { _, _ ->
                // User voted no, send response to server
                val gson = Gson()
                val gameRequestObject = CheaterInfo(data.gameId, name, "no", data.playerName)
                val cheaterInfo = gson.toJson(gameRequestObject)
                clientSocket.send("kick_out_player", cheaterInfo)
            }
            .setCancelable(false) // Optional: to make sure the user votes
            .show()
    }

    private val processGetTime = Emitter.Listener { args ->
        //Log.i("TimerData1", "${args[0]}")
        if (args[0] != null) {
            //Log.i("TimerData55", "${args[0]}")
            runOnUiThread {
                // Receive time from the server as an integer
                serverTime = args[0] as Int
               // Log.i("TimerData3", "$serverTime")
                val timerView: TextView = findViewById(R.id.time)

                // Convert the time to minutes and seconds
                val minutes = serverTime / 60
                val seconds = serverTime % 60
                val formattedTime = String.format("%02d:%02d", minutes, seconds)

                // Update the timer view with the formatted time
                timerView.text = formattedTime

                //Check if the game should end
                if (serverTime <= 0) {
                    endGame = true
                    clientSocket.removeListener("time")
                    //handleGameEnd()
                }
            }
        }
    }


    private fun leaveGame() {
        val leavingIntent = Intent(this, MainPageActivity::class.java)
        clientSocket.send("leave_game", data.gameId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        clientSocket.removeListener("click_personal")
        clientSocket.removeListener("click_enemy")
        clientSocket.removeListener("next_card")
        clientSocket.removeListener("cheat")
        clientSocket.removeListener("get_game_values")
        clientSocket.removeListener("game_values")
        clientSocket.removeListener("chat_message")
        clientSocket.removeListener("deserter")
        clientSocket.removeListener("cheat_alert")
        countDownTimer?.cancel()
        startActivity(leavingIntent)
        //finish()
    }

    private fun handleCheating() {
        isCheating = !isCheating
        if(isCheating == true){
            //Log.i("checkingchecking", "sent request")
            clientSocket.send("get_cheats", data.gameId)
            //send name of cheater to server here
        } else {
            //stopFlashing()
        }
    }

    private fun stopFlashing() {
        myJob.cancel() // This cancels all coroutines started by myScope
        myJob = Job() // Reset the job for future use
        myScope = CoroutineScope(Dispatchers.Main + myJob) // Recreate the scope with the new job
    }


    private val cheatFlashingOverlay = Emitter.Listener { args ->
        //Log.i("checkingchecking", "BEFORE IF")
        if (args[0] != null) {
           // Log.i("checkingchecking", "AFTER IF")
            runOnUiThread {
                val dataArray = args[0] as JSONArray  // Cast the first argument to JSONArray

                // Convert the JSONArray to a List<String>
                val stringList = mutableListOf<String>()
                for (i in 0 until dataArray.length()) {
                    stringList.add(dataArray.getString(i))
                }
                //Log.i("checkingchecking", "$stringList")
                flashingOverlay(stringList)
            }
        }
    }
    private val processNextCardEvent = Emitter.Listener { args ->
       // Log.i("RECIEVED NEXT CARD", "BEFORE IF")
        if (args[0] != null) {
           // Log.i("RECIEVED NEXT CARD", "AFTER IF")
            runOnUiThread {
                val dataLimitedTime = args[0] as JSONObject
               // Log.i("Données à changer", "${dataLimitedTime}")
                var name = dataLimitedTime.getString("name")
                var originalImage = dataLimitedTime.getString("originalImage")
                val decodedString: ByteArray = Base64.decode(originalImage, Base64.DEFAULT)
                var bitmapImageOriginal = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                var modifiedImage = dataLimitedTime.getString("modifiedImage")
                val decodedString1: ByteArray = Base64.decode(modifiedImage, Base64.DEFAULT)
                var bitmapImageModified = BitmapFactory.decodeByteArray(decodedString1, 0, decodedString1.size)
                var nbrDifference = dataLimitedTime.getString("nbDifferences")
                val newCard = CardFiles(
                    name = name,
                    originalImage = bitmapImageOriginal,
                    modifiedImage = bitmapImageModified,
                    nbDifferences = nbrDifference
                )
                nextCardFiles.add(newCard)
            }

        }
    }

    private fun displayErrorMessage(x: Float, y: Float, isModified:Boolean) {

        if(x == 0.0f && y == 0.0f) {
            return
        }

        val originalImageView = findViewById<ImageView>(R.id.originalImage)
        val modifiedImageView = findViewById<ImageView>(R.id.modifiedImage)
        // Calculate the screen coordinates relative to the originalImageView
        val originalImageLocation = IntArray(2)
        originalImageView.getLocationOnScreen(originalImageLocation)
        val originalImageX = originalImageLocation[0]
        val originalImageY = originalImageLocation[1]

        // Calculate the offset from the originalImageView's top-left corner
        val offsetXOriginal = originalImageX - x
        val offsetYOriginal = originalImageY - y


        // Inflate the custom error message layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val customView = inflater.inflate(R.layout.custom_error_message, null)

        val errorMessageText = customView.findViewById<TextView>(R.id.errorMessage)
        errorMessageText.text = "Erreur"

        // Create PopupWindows for both original and modified images
        val popupWindowOriginal = PopupWindow(
            customView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        val verticalOffset = -490

        // Show the PopupWindows at the specified offsets
        if(isModified) {
            //Log.i("kbm", "${y}")
            if(y.toInt() < 168) {
                popupWindowOriginal.showAsDropDown(modifiedImageView, x.toInt(), y.toInt()+verticalOffset)
            }
            else{
                popupWindowOriginal.showAsDropDown(modifiedImageView, x.toInt(), y.toInt())
            }
        }
        else if(!isModified) {
            //Log.i("kbm", "${y.toInt()}")
            if(y.toInt() < 168) {
                popupWindowOriginal.showAsDropDown(originalImageView, x.toInt(), y.toInt()+verticalOffset)
            }
            else{
                popupWindowOriginal.showAsDropDown(originalImageView, x.toInt(), y.toInt())
            }
        }

        // Dismiss the error message after a short delay
        Handler().postDelayed({
            popupWindowOriginal.dismiss() }, 1000) // Dismiss after 1 second

    }


    private val processClickResponse = Emitter.Listener { d ->
        //Log.i("RECIEVED", "BEFORE IF")
        if (d[0] != null) {
            //Log.i("RECIEVED", "AFTER IF")
            runOnUiThread {
              //  Log.i("RECIEVED", "AFTER RUN ON UI THREAD")
                val d = d[0] as JSONObject
                var playerName:String?
                val valid: Boolean = d.getBoolean("valid") // will be false if not present
                val penaltyTime: Int = d.optInt("penaltyTime", -1) // will be -1 if not present

                //Log.i("PARSED", "$d")
                if(valid == false) {
                    // add replay Events
                    playerActionsFalse.add(PlayerClickFalse(x, y, modified, serverTime.toLong()))

                    playerName = d.optString("playerName")
                    val current = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val formattedTime = current.format(formatter)

                    timeArray.add(formattedTime)
                    if(playerName != "") {
                        val objectMessage = message("Erreur par : ", playerName, true, false, false, true, false, true)
                        messageArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(messageArray.size -1)

                        //replay
                        gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
                    }
                    else if (playerName == "") {
                        val objectMessage = message("Erreur par : ", data.playerName, true, false, false, true, false, true)
                        messageArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(messageArray.size -1)
                        displayErrorMessage(x, y, modified)

                        //replay
                        gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
                    }
                    playErrorAudio()
                    //displayErrorMessage(x, y, modified)
                }

                if (valid && gameModeObserver == 0) {
                    val differenceNaturalOverlay: String = d.getString("differenceNaturalOverlay")
                    val differenceFlashOverlay: String = d.getString("differenceFlashOverlay")
                    playerName = d.optString("playerName")
                    var difficulty = data.difficulty
                    val differenceFlashOverlays = listOf(differenceFlashOverlay)
                    flashingOverlay(differenceFlashOverlays)
                    //Log.i("rdrd", "reached flash")
                    //playerActions for replay
                    playSucessAudio()
                    addDifferenceOverlay(differenceNaturalOverlay)

                    val current = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val formattedTime = current.format(formatter)
                    timeArray.add(formattedTime)
                    if(playerName != "") {
                        val objectMessage = message("Différence trouvée par : ", playerName, true, false, false, true, false, true)
                        messageArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(messageArray.size -1)

                        //replay
                        playerActionsValid.add(PlayerClickValid(x, y, serverTime.toLong(), differenceNaturalOverlay, differenceFlashOverlays, playerName))
                        gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
                    }
                    else if (playerName == "") {
                        val objectMessage = message("Différence trouvée par : ", data.playerName, true, false, false, true, false, true)
                        messageArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(messageArray.size -1)

                        //replay
                        playerActionsValid.add(PlayerClickValid(x, y, serverTime.toLong(), differenceNaturalOverlay, differenceFlashOverlays, data.playerName))
                        gameMessage.add(inGameMessage(objectMessage, serverTime.toLong(), formattedTime))
                    }
                    if (playerName != null) {
                        incrementScore(playerName)
                    }
                }

                if (valid == true && gameModeObserver == 2) {
                    playerName = d.optString("playerName")
                    if (nextCardFiles.isNotEmpty()) {
                        val nextCard = nextCardFiles.removeAt(0)
                        originalImageView.setImageBitmap(nextCard.originalImage)
                        modifiedImageView.setImageBitmap(nextCard.modifiedImage)
                    }

                    val current = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val formattedTime = current.format(formatter)
                    timeArray.add(formattedTime)
                    if(playerName != "") {
                        val objectMessage = message("Différence trouvée par : ", playerName, true, false, false, true, false, true)
                        messageArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(messageArray.size -1)
                    }

                    else if (playerName == "") {
                        val objectMessage = message("Différence trouvée par : ", data.playerName, true, false, false, true, false, true)
                        messageArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(messageArray.size -1)
                    }

                    playSucessAudio()
                    if (playerName != null) {
                        incrementScore(playerName)
                    }
                }

            }
        }
    }



    fun calculateFinalScore(): Int {
        var finalScore = 0

        // Supposons que gamePlayerScore contient les scores des joueurs
        for (score in gamePlayerScore.values) {
            finalScore += score
        }

        return finalScore
    }

    fun initAndUpdateScores(){
        val nameTextViews = listOf(
            findViewById<TextView>(R.id.playerName1),
            findViewById<TextView>(R.id.playerName2),
            findViewById<TextView>(R.id.playerName3),
            findViewById<TextView>(R.id.playerName4)
        )

        val scoreTextViews = listOf(
            findViewById<TextView>(R.id.playerScore1),
            findViewById<TextView>(R.id.playerScore2),
            findViewById<TextView>(R.id.playerScore3),
            findViewById<TextView>(R.id.playerScore4)
        )

        for (i in nameTextViews.indices) {
            if (i < data.playerNames.size) {
                val playerName = data.playerNames[i]
                val playerScore = gamePlayerScore[playerName]

                nameTextViews[i].text = playerName
                scoreTextViews[i].text = playerScore.toString()

                nameTextViews[i].visibility = View.VISIBLE
                scoreTextViews[i].visibility = View.VISIBLE
            } else {
                nameTextViews[i].visibility = View.GONE
                scoreTextViews[i].visibility = View.GONE
            }
        }
    }

    fun flashingOverlay(base64FlashOverlays: List<String>) {
        myScope.launch {
            val interval = 120L
            val parentFrameOriginal = findViewById<FrameLayout>(R.id.originalFrame)
            val parentFrameModified = findViewById<FrameLayout>(R.id.modifiedFrame)
            val overlayModified = findViewById<ImageView>(R.id.differenceOverlayModified)
            val layoutParams = overlayModified.layoutParams as FrameLayout.LayoutParams
            var idx = 0
            if (isCheating == true) {
                while (isActive && isCheating) {
                    for (base64FlashOverlay in base64FlashOverlays) {

                        if (base64FlashOverlay != null && base64FlashOverlay != "null") {
                           // Log.i("checkingchecking", "$base64FlashOverlay")
                            //Log.i("checkingchecking isCheating", "$isCheating")
                            val decodedStringOverlay =
                                android.util.Base64.decode(base64FlashOverlay, android.util.Base64.DEFAULT)
                            val flashBitmap = BitmapFactory.decodeByteArray(
                                decodedStringOverlay,
                                0,
                                decodedStringOverlay.size
                            )
                            val delay =interval
                            flashOnce(flashBitmap, delay, parentFrameOriginal, parentFrameModified, layoutParams)
                        }
                        if (!isActive || !isCheating){
                            break
                        }
                    }
                }
            } else {
                for (base64FlashOverlay in base64FlashOverlays) {
                    for (i in 0 until 5) {
                        val decodedStringOverlay =
                            android.util.Base64.decode(base64FlashOverlay, android.util.Base64.DEFAULT)
                        val flashBitmap = BitmapFactory.decodeByteArray(
                            decodedStringOverlay,
                            0,
                            decodedStringOverlay.size
                        )
                        val delay = i * interval
                        flashOnce(flashBitmap, delay, parentFrameOriginal, parentFrameModified, layoutParams)
                    }
                }
            }
        }
    }


    suspend fun flashOnce(
        flashBitmap: Bitmap, delay: Long, parentFrameOriginal: FrameLayout, parentFrameModified: FrameLayout, layoutParams: FrameLayout.LayoutParams
    ) {
       // Log.i("checkingchecking isCheating", "inside Flash once")
        delay(delay)
        val flashImageViewOrg = ImageView(parentFrameOriginal.context).apply {
            setImageBitmap(flashBitmap)
            this.layoutParams = layoutParams
        }
        val flashImageViewMod = ImageView(parentFrameModified.context).apply {
            setImageBitmap(flashBitmap)
            this.layoutParams = layoutParams
        }
        parentFrameOriginal.addView(flashImageViewOrg)
        parentFrameModified.addView(flashImageViewMod)

        delay(120)
        parentFrameOriginal.removeView(flashImageViewOrg)
        parentFrameModified.removeView(flashImageViewMod)

    }
    private fun playSucessAudio() {
        if(soundSucess == 1) {
            mediaPlayer = MediaPlayer.create(this, R.raw.success)
            mediaPlayer?.start()
        }
        else if (soundSucess == 2) {
            mediaPlayer = MediaPlayer.create(this, R.raw.success1)
            mediaPlayer?.start()
        }
    }

    private fun playErrorAudio() {
        if(soundError == 3) {
            mediaPlayer = MediaPlayer.create(this, R.raw.error)
            mediaPlayer?.start()
        }
        else if (soundError == 4) {
            mediaPlayer = MediaPlayer.create(this, R.raw.error1)
            mediaPlayer?.start()
        }
    }


    fun incrementScore(playerName: String) {
        var username = playerName
        if (playerName == null || playerName == ""){ username = data.playerName}
        // Increment the score in the map
        newScore = (gamePlayerScore[username] ?: 0) + 1
        gamePlayerScore[username] = newScore
        //Log.i("uuu", "$data")
        //Log.i("uuu", "${data.playerName}")
        //Log.i("uuu", "$username")
        //Log.i("uuu", "$gamePlayerScore")



        // Update the UI
        when (username) {
            data.playerNames.getOrNull(0) -> findViewById<TextView>(R.id.playerScore1).text = newScore.toString()
            data.playerNames.getOrNull(1) -> findViewById<TextView>(R.id.playerScore2).text = newScore.toString()
            data.playerNames.getOrNull(2) -> findViewById<TextView>(R.id.playerScore3).text = newScore.toString()
            data.playerNames.getOrNull(3) -> findViewById<TextView>(R.id.playerScore4).text = newScore.toString()
        }
    }


    fun addDifferenceOverlay(base64Overlay: String){
       // Log.i("addDifferenceOverlay", "INSIDE")

        val decodedStringOverlay = Base64.decode(base64Overlay, Base64.DEFAULT)
        val differenceBitmap = BitmapFactory.decodeByteArray(decodedStringOverlay, 0, decodedStringOverlay.size)

        val differenceImageView = ImageView(this)
        val differenceImageView2 = ImageView(this)
        differenceImageView.setImageBitmap(differenceBitmap)
        differenceImageView2.setImageBitmap(differenceBitmap)

        var overlayModified = findViewById<ImageView>(R.id.differenceOverlayModified)
        val layoutParams = overlayModified.layoutParams as FrameLayout.LayoutParams

        val layoutParams2 = overlayModified.layoutParams as FrameLayout.LayoutParams
        differenceImageView.layoutParams = layoutParams
        differenceImageView2.layoutParams = layoutParams2
        originalFrame.addView(differenceImageView)
        modifiedFrame.addView(differenceImageView2)
    }


    ////////////////////////////////// Beginning of replay functions ////////////////////////
    fun updateUI(remainingSeconds: Long) {
        // Update your UI elements, e.g., a TextView showing the remaining time
        val remainingTimeTextView: TextView = findViewById(R.id.time)
        remainingTimeTextView.text = remainingSeconds.toString()
    }

    fun checkAndDisplayActionsOrMessages(elapsedTime: Int) {
        // Check if there are actions or messages at the current time
        displayActionsFalse(playerActionsFalse, currentIndexFalse, elapsedTime)
        displayActionsValid(playerActionsValid, currentIndexValid, elapsedTime)
        displayMessages(gameMessage, currentIndexMessages, elapsedTime)
    }

    //replay
    fun displayErrorMessageIfTimeMatches(actions: List<PlayerClickFalse>, currentTimer: Int) {
        for (action in actions) {
            if (action.time.toInt() == currentTimer) {
                displayErrorMessage(action.x, action.y, action.isModified)
                playErrorAudio()
            }
        }
    }

    fun displayValidMessageIfTimeMatches(actions: List<PlayerClickValid>, currentTimer: Int) {
        for (action in actions) {
            if (action.time.toInt() == currentTimer) {
                flashingOverlay(action.differenceFlashOverlays)
                playSucessAudio()
                addDifferenceOverlay(action.flashOriginal)
                incrementScore(action.name)
            }
        }
    }

    fun displayTextMessages(actions: List<inGameMessage>, currentTimer: Int) {
        for (action in actions) {
            if (action.time.toInt() == currentTimer) {
                messageArray.add(action.message)
                timeArray.add(action.stringTime)
                chatAdapter.notifyDataSetChanged()
                chatRecycler.scrollToPosition(messageArray.size -1)
            }
        }
    }

    fun displayActionsValid(
        actions: List<PlayerClickValid>,
        currentIndex0: Int,
        elapsedTime: Int
    ) {
        var currentIndex = currentIndex0
        while (currentIndex < actions.size && actions[currentIndex].time.toInt() == elapsedTime) {
            // Display action based on the provided data
            displayActionValid(actions[currentIndex])
            currentIndex++
        }
    }

    fun displayActionsFalse(
        actions: List<PlayerClickFalse>,
        currentIndex0: Int,
        elapsedTime: Int
    ) {
        var currentIndex = currentIndex0
        while (currentIndex < actions.size && actions[currentIndex].time.toInt() == elapsedTime) {
            // Display action based on the provided data
            displayActionFalse(actions[currentIndex])
            currentIndex++
        }
    }

    fun displayMessages(
        actions:List<inGameMessage>,
        currentIndex0: Int,
        elapsedTime:Int
    ) {
        var currentIndex = currentIndex0
        while (currentIndex < actions.size && actions[currentIndex].time.toInt() == elapsedTime) {
            // Display action based on the provided data
            displayMessage(actions[currentIndex])
            currentIndex++
        }
    }

    fun displayActionValid(action: PlayerClickValid) {
        //flashOnce(action.flashOriginal)
    }

    fun displayActionFalse(action: PlayerClickFalse) {
        // Display the action based on the provided data
        displayErrorMessage(action.x, action.y, action.isModified)
    }

    fun displayMessage(message: inGameMessage) {
        // Display the message based on the provided data
        // ...
    }

    fun finishReplay() {
        // Perform any actions needed when the replay is finished
        // ...
    }


    override fun onDestroy() {
        super.onDestroy()
        myJob.cancel()
    }

    companion object {
        lateinit var data: gameData4
        var soundSucess: Int = 1
        var soundError: Int = 3
        var gameModeObserver: Int = 0
    }

}

private fun CoroutineScope.delay(i: Int) {

}
