package com.example.jeudifferences

import SocketClientHandler
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jeudifferences.databinding.CarrouselItemBinding
import com.google.gson.Gson
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Arrays


class CarouselAdapter(private val cardList: List<GameInfo>, val playerName:String, val context: Context): RecyclerView.Adapter<CarouselAdapter.GameViewHolder>() {
    class GameViewHolder(val binding: CarrouselItemBinding) : RecyclerView.ViewHolder(binding.root)
    val socketClient = SocketClientHandler
    val chosenPlayers = ArrayList<String>()
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
    var isSettingConfirmed = false;
    lateinit var players: JSONArray
    var awaitingPlayers: MutableMap<String, String> = mutableMapOf()
    lateinit var alertDialog: AlertDialog
    var alertAwaiting: AlertDialog? = null
    val playerNames = ArrayList<String>()
    lateinit var chatRecycler: RecyclerView
    lateinit var decodedStringModified: ByteArray
    lateinit var bitmapModifiedImage: Bitmap
    lateinit var stream1: ByteArrayOutputStream
    lateinit var byteArray1: ByteArray
    lateinit var decodedStringOriginal: ByteArray
    lateinit var bitmapOriginalImage: Bitmap
    lateinit var stream: ByteArrayOutputStream
    lateinit var byteArray:ByteArray


        enum class ResponseType(val type: String) {
            Starting("0"),
            Pending("1"),
            Cancelled("2"),
            Rejected("3")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = CarrouselItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameViewHolder(binding)
    }

    override fun getItemCount(): Int {
       return cardList.size
    }

    fun matchSetting(gameId: String) {
        Log.i("zazazaza", "matchSetting Called")
        // Assuming 'context' is obtained from a view or passed into the adapter
        val checkBox = CheckBox(context).apply {
            text = "Est-ce que la tricherie est permis?"
        }

        val input = EditText(context).apply {
            hint = "Temps du compte à rebours"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(input)
            addView(checkBox)
            // Add padding or other layout parameters if necessary
        }
        socketClient.on("player_status", getPlayerInfo)

        AlertDialog.Builder(context).apply {
            setView(layout)
            setCancelable(false)
            setPositiveButton("Confirmer") { dialog, which ->
                isSettingConfirmed = true
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
                Log.i("zazaza", settings.toString())
                val jsonSettings = gson.toJson(settings)
                socketClient.send("send_game_settings", jsonSettings)
                handlePendingCase(gameId)
                dialog.dismiss()

            }
            show()
        }
    }

    fun waitingPlayersDialogue(gameId: String){
        alertAwaiting?.dismiss()

        val builder = AlertDialog.Builder(context)

        builder.setMessage("En attente d'autres joueurs ")
        builder.setCancelable(false)
        builder.setNegativeButton("Quitter") { dialog, _ ->
            dialog.dismiss()
            (context as? ClassicSelectionActivity)?.let { activity ->
                activity.timeArray.clear()
                activity.messageArray.clear()
                activity.chatAdapter.notifyDataSetChanged()
            }
            socketClient.removeListener("player_status")
            socketClient.removeListener("response_to_play_request")
            (context as Activity).findViewById<FrameLayout>(R.id.chatLayoutFrame).visibility = View.GONE
            socketClient.send("leave_game", gameId)
        }

        alertAwaiting = builder.create()
        alertAwaiting?.show()

        // Adjust the width of the AlertDialog after showing it
        val window = alertAwaiting?.window
        if (window != null) {
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.45).toInt() // Set the dialog width to 75% of screen width, adjust as needed
            window.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    fun handlePendingCase(gameId: String){
        if(waitingPlayers.size == 0) {
            (context as Activity).findViewById<FrameLayout>(R.id.chatLayoutFrame).visibility = View.VISIBLE
            waitingPlayersDialogue(gameId)
        }
        else
            showListOfPlayersDialogue()
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = cardList[position]
        holder.binding.apply {
            Glide.with(imageGame).load(game.image).into(imageGame)
            titre.text = game.gameName
            difficulty.text = game.difficulty

            if(game.difficulty == "Difficile") {
                backgroundCardColor.setBackgroundResource(R.color.pinkC)
            }

            nameMulti1.text = game.multiBestTimes?.firstPlace?.name
            nameMulti2.text = game.multiBestTimes?.secondPlace?.name
            nameMulti3.text = game.multiBestTimes?.thirdPlace?.name

            nameSolo1.text = game.soloBestTimes?.firstPlace?.name
            nameSolo2.text = game.soloBestTimes?.secondPlace?.name
            nameSolo3.text = game.soloBestTimes?.thirdPlace?.name

            timeMulti1.text = "- "+ String.format("%02d", game.multiBestTimes?.firstPlace?.time?.minutes) + ":" + String.format("%02d", game.multiBestTimes?.firstPlace?.time?.seconds)
            timeMulti2.text = "- "+ String.format("%02d", game.multiBestTimes?.secondPlace?.time?.minutes) + ":" + String.format("%02d", game.multiBestTimes?.secondPlace?.time?.seconds)
            timeMulti3.text = "- "+ String.format("%02d", game.multiBestTimes?.thirdPlace?.time?.minutes) + ":" + String.format("%02d", game.multiBestTimes?.thirdPlace?.time?.seconds)

            timeSolo1.text = "- "+ String.format("%02d", game.soloBestTimes?.firstPlace?.time?.minutes) + ":" + String.format("%02d", game.soloBestTimes?.firstPlace?.time?.seconds)
            timeSolo2.text = "- "+ String.format("%02d", game.soloBestTimes?.secondPlace?.time?.minutes) + ":" + String.format("%02d", game.soloBestTimes?.secondPlace?.time?.seconds)
            timeSolo3.text = "- "+ String.format("%02d", game.soloBestTimes?.thirdPlace?.time?.minutes) + ":" + String.format("%02d", game.soloBestTimes?.thirdPlace?.time?.seconds)


            playButton2.setOnClickListener {
               socketClient.connect()
               gameID = game.gameId.toString()
               //Log.i("helloo", "$gameID")
               val gson = Gson()
               val gameRequestObject = game.gameId?.let { it1 -> gameData2(0, it1, playerName) }
               val jsonString = gson.toJson(gameRequestObject)
               socketClient.on("response_to_play_request", classic1vs1)
               socketClient.send("request_to_play", jsonString)


                // à enlever à la fin avant la remise
                Toast.makeText(holder.binding.root.context, game.gameId, Toast.LENGTH_SHORT).show()
           }
        }
    }

    private val classic1vs1 = Emitter.Listener() { args ->
        if (args[0] != null) {
            Handler(Looper.getMainLooper()).post(Runnable() {
                run() {
                    val data = args[0] as JSONObject
                    //Log.i("hello", "$data")
                    val responseTime = data.getString("responseType")
                    gameId = data.getString("gameId")


                    when (responseTime) {
                        ResponseType.Starting.type -> {
                            //startGame
                            alertAwaiting?.dismiss()
                            playerNbr = data.getString("playerNbr")
                            timetillStart = data.getString("startingIn")
                            val gameValues = data.getJSONObject("gameValues")
                            modifiedImage = data.getString("modifiedImage")
                            originalImage = data.getString("originalImage")
                            differenceNbr = data.getString("differenceNbr")
                            difficulty = data.getString("difficulty")

                            gameName = data.getString("gameName")
                            hostName = data.getString("hostName")
                            players = data.getJSONArray("players")
                            for (i in 0 until players.length()) {
                                playerNames.add(players.get(i) as String)
                                //Log.i("eww", "$playerNames")
                            }
                            val gson = Gson()
                            val gameRequestObject = gameData3(playerNames, gameId, true)
                            val jsonString = gson.toJson(gameRequestObject)
                            socketClient.send("validate_player", jsonString)

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

                            removeListeners()

                            GamePageActivity.data = gameData4(playerName,playerNbr, gameId,gameName,difficulty,
                                differenceNbr, byteArray, byteArray1, timetillStart, playerNames, 0, gameValues)
                            val Intent = Intent(context, GamePageActivity::class.java)
                            startActivity(context, Intent, null)
                        }

                        ResponseType.Pending.type -> {
                            val isSettingSpecified = data.getBoolean("isSettingSpecified")
                            //Log.i("zazazaaaa", "$isSettingSpecified")
                            if (isSettingSpecified == false) {
                                matchSetting(gameId)
                            }
                            else {handlePendingCase(gameId)}

                        }

                        ResponseType.Cancelled.type -> {

                        }

                        ResponseType.Rejected.type -> {
                            alertAwaiting?.dismiss()
                            val alertRefused = AlertDialog.Builder(context)
                            alertRefused.setMessage("Le joueur a refusé de jouer avec vous")
                            alertRefused.setCancelable(false)
                            alertRefused.setNegativeButton("Ok") { dialog: DialogInterface, _: Int ->
                                dialog.dismiss()
                            }
                            alertRefused.create().show()
                        }
                    }
                }
            })
        }
    }

    fun showListOfPlayersDialogue() {
        if (waitingPlayers.size >= 1) {
            val listItems: Array<CharSequence> = waitingPlayers.toTypedArray()
            val checkedItems = BooleanArray(waitingPlayers.size)
            val builder = AlertDialog.Builder(context)

            builder.setTitle("En attente de joueurs")

            builder.setMultiChoiceItems(listItems, checkedItems) { _, i, isChecked ->
                checkedItems[i] = isChecked
                updateStartButtonState(checkedItems)
            }
            builder.setCancelable(false)
            builder.setNegativeButton("Quitter") { dialog, _ ->
                socketClient.send("leave_game", gameId)
                socketClient.removeListener("player_status")
                socketClient.removeListener("response_to_play_request")
                dialog.dismiss()
            }

            builder.setPositiveButton("Commencer") { dialog, _ ->
                dialog.dismiss()
                chosenPlayers.clear()
                for (i in listItems.indices) {
                    if (checkedItems[i]) {
                        val item = listItems[i]

                        val idChosenPlayer = awaitingPlayers[item as String]
                        checkedItems[i] = false
                        if (idChosenPlayer != null) {
                            chosenPlayers.add(idChosenPlayer)
                        }
                    }
                }

                val gson = Gson()
                val gameRequestObject = gameData3(chosenPlayers, gameId, true)
                val jsonString = gson.toJson(gameRequestObject)
                socketClient.send("validate_player", jsonString)
                dialog.dismiss()
            }

            alertDialog = builder.create()
            alertDialog.show()

            // Initially disable the "Commencer" button
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
    }

    private fun updateStartButtonState(checkedItems: BooleanArray) {
        val isAnyItemSelected = checkedItems.any { it }
        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = isAnyItemSelected
    }


    private val getPlayerInfo = Emitter.Listener() { d ->
        if (d[0] != null) {

            Handler(Looper.getMainLooper()).post(Runnable() {
                run() {
                    val data = d[0] as JSONObject
                    val playerStatus = data.getString("playerConnectionStatus")
                    val user = data.getJSONObject("user")
                    val name = user.getString("name")
                    val id = user.getString("id")

                    //Log.i("zazazaaaa", "$data")

                    if (playerStatus == 0.toString()) {
                        waitingPlayers.add(name)
                        awaitingPlayers.put(name, id)
                        ////Log.i("hii", "$waitingPlayers")
                    }

                    if (playerStatus == 1.toString()) {
                        alertDialog.dismiss()
                        waitingPlayers.remove(name)
                        awaitingPlayers.remove(name)
                        if(waitingPlayers.size == 0) waitingPlayersDialogue(gameId)

                    }


                    if (isSettingConfirmed) {
                        showListOfPlayersDialogue()
                    }

                }
            })
        }
    }

    fun removeListeners() {
        socketClient.removeListener("player_status")
        socketClient.removeListener("request_to_play")
        socketClient.removeListener("response_to_play_request")
        socketClient.removeListener("validate_player")
    }

}
