package com.example.jeudifferences

import SocketClientHandler
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.ContactsContract
import android.widget.RelativeLayout
import androidx.core.graphics.toColor
import androidx.core.net.toUri
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.Date
import java.util.Locale
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Call
import java.io.FileInputStream
import java.io.IOException
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener


class ChatActivity : AppCompatActivity() {
    lateinit var chatRecycler: RecyclerView
    lateinit var sendBtn: ImageButton
    lateinit var listActivePlayersButton: ImageButton
    lateinit var messageBox: EditText
    lateinit var chatAdapter: ChatAdapter
    lateinit var messageArray: ArrayList<message>
    lateinit var principalMessagesArray: ArrayList<principalChatMessage>
    lateinit var timeArray: ArrayList<String>
    var clientSocket = SocketClientHandler
    lateinit var quitButton: Button
    private var mediaPlayer: MediaPlayer? = null
    var activePlayers: MutableMap<String, String> = mutableMapOf()
    private val soundResourceId = R.raw.sound
    lateinit var idChatPrincipale: String
    lateinit var selectedColor: ColorObject
    lateinit var view: View
    lateinit var pickColorLayout: RelativeLayout
    lateinit var pickColorButton: ImageButton
    lateinit var sendVocalMessage: ImageButton
    lateinit var stopRecordingView:ImageButton
    private lateinit var mediaRecorder: MediaRecorder
    var tempMediaOutput: String = ""
    var mediaState: Boolean = false
    private lateinit var voiceRecorder: VoiceRecorderActivity
    private var audioData: ByteArray? = null
    private var  REQUEST_RECORD_AUDIO_PERMISSION = 1;



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //pickColorLayout = findViewById(R.id.pickColor)
        //pickColorButton = findViewById(R.id.colorPickerIcon)
        //colorPickerView = findViewById(R.id.colorPickerView)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)

        }

        //Log.i("playerName", "$name")
        sendVocalMessage = findViewById(R.id.sendVoices)
        stopRecordingView = findViewById(R.id.stopRecord)
        // set default tag to send button as mic icon
        // sendVocalMessage.tag = R.drawable.baseline_keyboard_voice_2

        chatRecycler = findViewById(R.id.messagesView)
        messageBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendMessageBtn)
        listActivePlayersButton = findViewById(R.id.listPlayersButton)
        var backToMain = findViewById<Button>(R.id.backToMain1)
        messageArray = ArrayList()
        principalMessagesArray = ArrayList()
        timeArray = ArrayList()
        chatAdapter = name.let { ChatAdapter(this, principalMessagesArray, it, timeArray) }
        chatRecycler.layoutManager = LinearLayoutManager(this)
        chatRecycler.adapter = chatAdapter

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)
        //Log.i("hiiiii", "$isGlobalChat")

        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            clientSocket.removeListener("load_chat")
            clientSocket.removeListener("new_message")
            clientSocket.removeListener("chat_message")
            clientSocket.removeListener("send_voice_message")
            clientSocket.removeListener("send_message")
            startActivity(mainPageActivity)
        }
        sendBtn.setOnClickListener {
            if(isGlobalChat) {
                sendGlobalMessage()
            }
        }

        if (isGlobalChat) {
            clientSocket.send("load_chat")
            clientSocket.on("load_chat", loadAllChats)
            clientSocket.on("new_message", onNewMessage)
            listActivePlayersButton.visibility = View.VISIBLE
            sendVocalMessage.visibility = View.VISIBLE
            stopRecordingView.visibility = View.GONE

        }


        /*if (!isGlobalChat) {
            clientSocket.on("chat_message", onMessage)
        }*/




        if (isGlobalChat) {
            sendGlobalMessage()
        }

        listActivePlayersButton.setOnClickListener {
            val listPlayersIntent = Intent(this, ListPlayersDialog::class.java)
            startActivity(listPlayersIntent)
        }

        messageBox.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                sendGlobalMessage()
                return@OnKeyListener true
            }
            false
        })

        //Log.i("load color pick", "picked color")
        //  loadColorPicker()
        //Log.i("load color pick after ", "picked color")
        //val outputDirectory = getOutputDirectory()
        //tempMediaOutput = "$outputDirectory/${Date().time}.mp3"
        voiceRecorder = VoiceRecorderActivity()
        //tempMediaOutput =
        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath + "/" + Date().time + ".mp3"

        //voiceRecorder = VoiceRecorderActivity(tempMediaOutput)
        sendVocalMessage.setOnClickListener {
            Log.d("ChatActivity", "Start recording clicked")
            sendVocalMessage.visibility = View.GONE;
            stopRecordingView.visibility = View.VISIBLE;
            voiceRecorder.startRecording()
        }
        stopRecordingView.setOnClickListener {
            Log.d("ChatActivity", "Stop recording clicked")
            stopRecordingView.visibility = View.GONE;
            sendVocalMessage.visibility = View.VISIBLE;
            audioData = voiceRecorder.stopRecordingAndGetAudioData() // Get the recorded data as byte array
            //Log.i("about",audioData.toString())
            sendVoiceNote()
        }
        val pickColorButton = findViewById<Button>(R.id.pickColorButton)

        pickColorButton.setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun showColorPickerDialog() {
        ColorPickerDialog.Builder(this)
            .setTitle("Choisir un couleur")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                val color = envelope.color
                val myRelativeLayout = findViewById<RelativeLayout>(R.id.myRelativeLayout)
                myRelativeLayout.setBackgroundColor(color)
            })
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .show()
    }



    fun fileToByteArray(filePath: String): ByteArray {
        val file = File(filePath)
        val fileInputStream = FileInputStream(file)
        val byteArray = ByteArray(file.length().toInt())
        fileInputStream.read(byteArray)
        fileInputStream.close()
        return byteArray
    }
    fun sendVoiceNote() {
        audioData?.let { data ->
            try {
                // Send the recorded audio data
                clientSocket.send("send_voice_message", JSONObject().apply {
                    put("chatId", "6565fecd98b4ea695ffe3197")
                    put("message", data)
                    put("sender", currentId)
                })
                //Log.i("message send", "message send")
                chatRecycler.scrollToPosition(data.size - 1)

            } catch (e: IOException) {
                Log.e("Socket", "Error sending voice message: ${e.message}", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

    }

    private fun playAudio() {
        //mediaPlayer = MediaPlayer.create(this, R.raw.sound)
        mediaPlayer?.start()
    }

    fun stopAudio() {
        mediaPlayer?.stop()
    }

    private fun showNotification() {
        // Créez et affichez une notification ici
        // Vous pouvez utiliser le NotificationManager pour créer une notification
    }

    private val onNewMessage = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val d = args[0] as JSONObject
                //Log.i("byee", "$d")
                val messageType = d.getString("type")
                val user = d.getString("sender")
                val message = d.getString("message")
                val time = d.getString("timestamp")
                val chatId = d.getString("chatId")
                val utcInstant = Instant.parse(time)
                val etZoneId = ZoneId.of("America/Toronto")
                val etZonedDateTime = ZonedDateTime.ofInstant(utcInstant, etZoneId)
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formattedTime = formatter.format(etZonedDateTime)

                if (messageType == "text") {
                    // Existing handling for text messages
                    if (user == currentId) {
                        timeArray.add(formattedTime)
                        val objectMessage = principalChatMessage(chatId, message, name, "text",user)
                        principalMessagesArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(principalMessagesArray.size - 1)
                    } else if (user != currentId) {
                        timeArray.add(formattedTime)
                        val userName = activePlayers[user].toString()
                        val objectMessage = principalChatMessage(chatId, message, userName, "text",user)
                        principalMessagesArray.add(objectMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(principalMessagesArray.size - 1)
                        playAudio()
                    }
                } else if (messageType == "audio") {
                    if (user == currentId) {
                        timeArray.add(formattedTime)
                        val voiceMessageUrl = d.getString("message") // URL of the voice message
                        val userName = activePlayers[user].toString()
                        val voiceMessage =
                            principalChatMessage(chatId, voiceMessageUrl, userName, "voice",user)
                        principalMessagesArray.add(voiceMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(principalMessagesArray.size - 1)


                    }else if (user != currentId) {
                        timeArray.add(formattedTime)
                        val voiceMessageUrl = d.getString("message")
                        val userName = activePlayers[user].toString()
                        val voiceMessage = principalChatMessage(chatId, voiceMessageUrl, userName, "voice",user )
                        principalMessagesArray.add(voiceMessage)
                        chatAdapter.notifyDataSetChanged()
                        chatRecycler.scrollToPosition(principalMessagesArray.size - 1)
                        playAudio()
                    }
                }
            }
        }
    }

    private val loadAllChats = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                try {
                    val chats = args[0] as JSONObject
                    // Assuming 'idChatPrincipale' and 'activePlayers' are already defined elsewhere in your code
                    idChatPrincipale = "6565fecd98b4ea695ffe3197"

                    if (chats.has("users") && !chats.isNull("users")) {
                        val users = chats.getJSONArray("users")
                        //Log.i("jjee", "$users")

                        for (i in 0 until users.length()) {
                            val user = users.getJSONObject(i)
                            if (user != null) {
                                val userId = user.optString("userId", "defaultUserId")
                                val username = user.optString("username", "defaultUsername")
                                activePlayers.put(userId, username)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    // Handle the JSON parsing exception here
                }
            }
        }
    }


    /*private val onMessage = Emitter.Listener() { args ->
       if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONObject
                var name = ""
                var message = ""
                var time = ""
                try {
                    name = data.getString("sender")
                    message = data.getString("message")
                    time = data.getString("time")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                timeArray.add(time)
                val objectMessage = message(message, name)
                messageArray.add(objectMessage)
                chatAdapter.notifyDataSetChanged()
                chatRecycler.scrollToPosition(messageArray.size -1)
                playAudio()
                showNotification()
            }
       }
    }*/

    fun sendGlobalMessage() {
        val current = LocalDateTime.now()
        var message = messageBox.text.toString().trim()
        if(message.isNotEmpty()) {
            val sentMessage = principalChatMessage("6565fecd98b4ea695ffe3197",message, currentId, "text")
            val gson = Gson()
            val jsonString = gson.toJson(sentMessage)
            clientSocket.send("send_message", jsonString)
        }
        messageBox.text.clear()
        chatRecycler.scrollToPosition(messageArray.size -1)
    }

    companion object {
        var isGlobalChat = false
        var name = ""
        var currentId = ""
        var isInchat = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
    }

}
