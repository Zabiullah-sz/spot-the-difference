package com.example.jeudifferences

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import SocketClientHandler
import android.provider.DocumentsContract
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
class CongratMessageClassic : AppCompatActivity() {
    lateinit var closeButton: Button
    lateinit var winnerView: TextView
    lateinit var replayButton: Button
    var isWinnerName:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientSocket = SocketClientHandler

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        val isWinner = intent.getBooleanExtra("isWinner", false)
        val playerName = intent.getStringExtra("playerName")
        val playerNameServer = intent.getStringExtra("playerNameServer")


        if (isWinner) {
            setContentView(R.layout.activity_congratmessage_classic)
            winnerView = findViewById(R.id.winnerName)
            winnerView.setText(playerName)
        }
         else {
             Log.i("you loose","you loose!")
            setContentView(R.layout.activity_congratmessage_looser)
            winnerView = findViewById(R.id.winnerName)
            winnerView.setText(playerNameServer)
        }
        closeButton = findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            val mainActivityIntent = Intent(this, MainPageActivity::class.java)
            startActivity(mainActivityIntent)
        }
        setFinishOnTouchOutside(false)
    }
}
