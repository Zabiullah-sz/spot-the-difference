package com.example.jeudifferences

import SocketClientHandler
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject


class GameConstantDialog: AppCompatActivity() {
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var editNumberParam1: EditText
    private lateinit var editNumberParam2: EditText
    private lateinit var editNumberParam3: EditText
    var clientSocket = SocketClientHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game_constant_dialog)

        cancelButton = findViewById(R.id.buttonCancelParams)
        saveButton = findViewById(R.id.buttonSaveParams)
        resetButton = findViewById(R.id.buttonResetParams)

        editNumberParam1 = findViewById(R.id.editNumberParam1)
        editNumberParam2 = findViewById(R.id.editNumberParam2)
        editNumberParam3 = findViewById(R.id.editNumberParam3)

        editNumberParam1.filters = arrayOf<InputFilter>(MinMaxFilter(30, 300))
        editNumberParam2.filters = arrayOf<InputFilter>(MinMaxFilter(1, 30))
        editNumberParam3.filters = arrayOf<InputFilter>(MinMaxFilter(1, 30))

        cancelButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            editNumberParam1.setText("30")
            editNumberParam2.setText("5")
            editNumberParam3.setText("5")
        }

        saveButton.setOnClickListener {
            val constants = JSONObject()
            constants.put("timerTime", editNumberParam1.text.toString())
            constants.put("penaltyTime", editNumberParam2.text.toString())
            constants.put("gainedTime", editNumberParam3.text.toString())
            clientSocket.send("set_game_values", constants)
            finish()
        }

        clientSocket.on("game_values", onGameValues)
        clientSocket.send("get_game_values")
    }

    private val onGameValues =
        Emitter.Listener { args ->
            runOnUiThread(Runnable {
                val data = args[0] as JSONObject
                val time: Number
                val penalty: Number
                val gain: Number
                try {
                    time = data.getInt("timerTime")
                    penalty = data.getInt("penaltyTime")
                    gain = data.getInt("gainedTime")

                    editNumberParam1.setText(time.toString())
                    editNumberParam2.setText(penalty.toString())
                    editNumberParam3.setText(gain.toString())
                } catch (e: JSONException) {
                    return@Runnable
                }
            })
        }
    inner class MinMaxFilter() : InputFilter {
        private var intMin: Int = 0
        private var intMax: Int = 0

        // Initialized
        constructor(minValue: Int, maxValue: Int) : this() {
            this.intMin = minValue
            this.intMax = maxValue
        }

        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dStart: Int, dEnd: Int): CharSequence? {
            try {
                val input = Integer.parseInt(dest.toString() + source.toString())
                if (isInRange(intMin, intMax, input)) {
                    return null
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            return ""
        }

        // Check if input c is in between min a and max b and
        // returns corresponding boolean
        private fun isInRange(a: Int, b: Int, c: Int): Boolean {
            return if (b > a) c in a..b else c in b..a
        }
    }
}
