package com.example.jeudifferences

import SocketClientHandler
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject

class ListPlayersDialog: AppCompatActivity() {
    var usersList = arrayListOf<Users>()
    private lateinit var carousel: RecyclerView
    var clientSocket = SocketClientHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_list_player_dialog)

        carousel = findViewById(R.id.carouselRecyclerviewPlayerList)
        carousel.layoutManager = LinearLayoutManager(this)

        val activePlayerCarousel = ActivePlayerCarousel(usersList, this)
        carousel.adapter = activePlayerCarousel

        clientSocket.on("all_active_users", updateActiveUsers)
        clientSocket.send("retrieve_all_active_users")
    }

    private val updateActiveUsers = Emitter.Listener() { args ->
        if (args[0] != null) {
            runOnUiThread {
                val data = args[0] as JSONArray
                usersList.clear()

                for (i in 0 until data.length()) {
                    val element = data.getJSONObject(i)
                    var username = element.get("username").toString()

                    var userId = element.get("userId").toString()

                    usersList.add(Users(username, userId))
                }
                carousel.adapter?.notifyDataSetChanged()
            }
        }
    }
}
