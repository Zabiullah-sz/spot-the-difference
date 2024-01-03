package com.example.jeudifferences

import SocketClientHandler
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jeudifferences.databinding.CarrouselItemBinding


class ConfigurationCarousel(): RecyclerView.Adapter<ConfigurationCarousel.GameViewHolder>() {

    class GameViewHolder(val binding: CarrouselItemBinding) : RecyclerView.ViewHolder(binding.root)
    val clientSocket = SocketClientHandler
    private lateinit var cardList: List<GameInfo>
    private lateinit var context: Context

    constructor(cardList: List<GameInfo>, context: Context) : this() {
        this.cardList = cardList
        this.context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = CarrouselItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return cardList.size
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = this.cardList[position]
        holder.binding.apply {
            Glide.with(imageGame).load(game.image).into(imageGame)
            titre.text = game.gameName
            difficulty.text = game.difficulty

            if(game.difficulty == "Difficile") {
                backgroundCardColor.setBackgroundResource(R.color.pinkC)
            } else {
                backgroundCardColor.setBackgroundResource(R.color.blue)
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


            playButton.text = "Supprimer"
            playButton2.text = "Réinitialiser Top 3"

            playButton.setOnClickListener {
                val dialog = WarnDialog(context)
                dialog.show("supprimer ce jeu?") { clientSocket.send("delete_card", game.gameId) }
            }

            playButton2.setOnClickListener {
                val dialog = WarnDialog(context)
                dialog.show("réinitialiser les meilleurs temps de ce jeu?") { clientSocket.send("reset_best_times", game.gameId) }
            }

        }
    }
}
