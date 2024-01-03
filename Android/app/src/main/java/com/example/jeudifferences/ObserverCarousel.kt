package com.example.jeudifferences;

import SocketClientHandler
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class ObserverCarousel(): RecyclerView.Adapter<ObserverCarousel.GameViewHolder>(){

    val clientSocket = SocketClientHandler
    private lateinit var gameList: List<ActiveGames>
    private lateinit var context: Context

    constructor(gameList: List<ActiveGames>, context: Context) : this() {
        this.gameList = gameList
        this.context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.observer_carousel_item, parent, false)
        return GameViewHolder(view)
    }

    override fun getItemCount(): Int {
        return gameList.size
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = this.gameList[position]
        holder.gamemode.text = game.gameMode
        holder.playerNbr.text = "Nombre de joueurs:" + game.playersNbr.toString()
        game.id?.let { Log.d("pp", it) }
        holder.join.setOnClickListener {
            val gameId = JSONObject()
            gameId.put("gameId", game.id);

            clientSocket.send("join_as_observer", gameId)
            if(game.gameMode == "Temps limité Coop") {
                ObserverGamePageActivity.gameModeObserver = 2
            }
            //case of classic time
            else{
                ObserverGamePageActivity.gameModeObserver = 0
            }
        }
    }

    class GameViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val gamemode: TextView = view.findViewById(R.id.carouselGameMode)
        val playerNbr: TextView = view.findViewById(R.id.playerNumberCarousel)
        val join: Button = view.findViewById(R.id.joinButton)
    }
}
