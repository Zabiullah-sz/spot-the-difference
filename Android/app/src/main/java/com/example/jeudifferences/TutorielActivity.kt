package com.example.jeudifferences

import SocketClientHandler
import ThemePreferences
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jeudifferences.databinding.ActivityTutorielBinding
import io.socket.emitter.Emitter
import org.json.JSONObject

class TutorielActivity : BaseActivity() {
    private lateinit var binding: ActivityTutorielBinding
    lateinit var chatButton:Button
    var clientSocket = SocketClientHandler
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorielBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setTheme()
        clientSocket.connect()
        mediaPlayer?.stop()
        ChatActivity.isInchat = false
        val backToMain = findViewById<Button>(R.id.backToMain5)
        backToMain.setOnClickListener {
            val mainPageActivity = Intent (this, MainPageActivity::class.java)
            mediaPlayer?.stop()
            clientSocket.removeListener("new_message")
            startActivity(mainPageActivity)
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)

        if (!ChatActivity.isInchat) {
            ChatActivity.isGlobalChat = true
            clientSocket.on("new_message", onNewMessage)
        }

        var tutorielImages = intArrayOf(
            R.drawable.tu1,
            R.drawable.tu2,
            R.drawable.tu3,
            R.drawable.tu4,
            R.drawable.tu5,
            R.drawable.tu6,
            R.drawable.tu7,
            R.drawable.tu8,
            R.drawable.tu9,
            R.drawable.tu10,
            R.drawable.tu11,
            R.drawable.tu12,
            R.drawable.tu13
        )
        chatButton = findViewById(R.id.ChatButton2)
        val adapter = TutorielAdapter(tutorielImages, this)
        binding.apply {
            tutorielRecyclerview.adapter = adapter
            tutorielRecyclerview.setInfinite(false)
        }

        chatButton.setOnClickListener {
            ChatActivity.isGlobalChat = true
            ChatActivity.isInchat = true
            mediaPlayer?.stop()
            val chatIntent = Intent (this, ChatActivity::class.java)
            clientSocket.removeListener("new_message")
            startActivity(chatIntent)
        }
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
                    playAudio()
                    Toast.makeText(this, "Nouveau Message!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        //mediaPlayer?.stop()
        clientSocket.on("new_message", onNewMessage)
    }

    private fun playAudio() {
        //mediaPlayer = MediaPlayer.create(this, R.raw.sound)
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
    }
}
