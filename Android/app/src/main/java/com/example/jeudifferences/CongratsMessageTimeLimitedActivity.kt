package com.example.jeudifferences

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class CongratsMessageTimeLimitedActivity : AppCompatActivity() {
    lateinit var closeButton: Button
    lateinit var differenceFoundView:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_congrat_message_limitedtime)
        val totalDifferences = intent.getIntExtra("final score", 0)

        closeButton = findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            val mainActivityIntent = Intent(this, MainPageActivity::class.java)
            startActivity(mainActivityIntent)
            //finish()
        }
        differenceFoundView= findViewById(R.id.differenceFound)
        differenceFoundView.setText(totalDifferences.toString())
    }

}
