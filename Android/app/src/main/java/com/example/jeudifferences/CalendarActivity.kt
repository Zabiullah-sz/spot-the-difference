package com.example.jeudifferences

import SocketClientHandler
import ThemePreferences
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar


class CalendarActivity: BaseActivity() {
    lateinit var addEventButton: Button
    lateinit var eventMessage: TextView
    lateinit var eventTime:TextView
    lateinit var chatButton:Button
    lateinit var backToMain:Button
    var clientSocket = SocketClientHandler
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        setTheme()
        addEventButton = findViewById(R.id.createEventButton)
        eventMessage = findViewById(R.id.eventBox)
        eventTime = findViewById(R.id.dateBox)
        chatButton = findViewById(R.id.ChatButton1)
        backToMain = findViewById(R.id.backToMain)
        clientSocket.connect()
        ChatActivity.isInchat = false

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

        if (!ChatActivity.isInchat) {
            ChatActivity.isGlobalChat = true
            clientSocket.on("new_message", onNewMessage)
        }

        addEventButton.setOnClickListener {
            addCalendarEvent()
        }
        chatButton.setOnClickListener {
            ChatActivity.isGlobalChat = true
            ChatActivity.isInchat = true
            mediaPlayer?.stop()
            val chatIntent = Intent (this, ChatActivity::class.java)
            clientSocket.removeListener("new_message")
            startActivity(chatIntent)
        }
        backToMain.setOnClickListener {
            mediaPlayer?.stop()
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            clientSocket.removeListener("new_message")
            startActivity(mainPageActivity)
        }
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer = MediaPlayer.create(this, R.raw.sound)
    }

    //inspiré de code trouvé en ligne
    private fun addCalendarEvent() {
        val dateString = eventTime.text.toString()
        val message = eventMessage.text.toString().trim()
        if(dateString.isNotEmpty() && message.isNotEmpty()) {
            if (isCorrectFormat(dateString)) {
                val dateFormat = SimpleDateFormat("MM-dd-yyyy-hh:mm")
                val date = dateFormat.parse(dateString)

                val intent = Intent(Intent.ACTION_INSERT)
                intent.type = "vnd.android.cursor.item/event"
                intent.putExtra(Events.TITLE, message)
                intent.putExtra(Events.EVENT_LOCATION, "")
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.time)
                intent.putExtra(Events.ALL_DAY, false)
                intent.putExtra(Events.STATUS, 1)
                intent.putExtra(Events.VISIBLE, 0)
                startActivity(intent)
                eventTime.text = ""
                eventMessage.text = ""
            }
            else{
                Toast.makeText(this, "Format de date invalide. Utilisez MM-dd-yyyy-hh:mm", Toast.LENGTH_SHORT).show()
            }
        }
        else if (message.isEmpty() || dateString.isEmpty()) {
            Toast.makeText(this, "Aucun message ou date rentrée", Toast.LENGTH_SHORT).show()
        }
    }

    fun isCorrectFormat(input: String): Boolean {
        val regexPattern = """\d{2}-\d{2}-\d{4}-\d{2}:\d{2}""".toRegex()
        return regexPattern.matches(input)
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
        mediaPlayer?.start()
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
