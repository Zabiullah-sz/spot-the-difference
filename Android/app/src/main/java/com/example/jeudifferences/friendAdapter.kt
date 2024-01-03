package com.example.jeudifferences

import SocketClientHandler
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jeudifferences.databinding.CarrouselItemBinding
import com.google.gson.Gson
import org.json.JSONObject
import java.net.URI

class friendAdapter(val context: Context, val friendArray: ArrayList<User>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.actif_player_item, parent, false)
        return activePlayersView(view)
    }
//
    override fun getItemCount(): Int {
        return friendArray.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = friendArray[position]
        val viewHolder = holder as activePlayersView
        Glide.with(context)
            .load("http://ec2-3-99-221-92.ca-central-1.compute.amazonaws.com:3000/api/users/get-public-profile-image/${currentMessage.userId}")
            .into(viewHolder.image)
        holder.name.text = currentMessage.username
        holder.sendButton.setOnClickListener {
            val clientSocket = SocketClientHandler
            val gson = Gson()
            clientSocket.connect()
            val payload = JSONObject().apply {
                put("username", currentMessage.username.toString())
            }
            clientSocket.send("friend_request", payload)

        }

    }

    class activePlayersView (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.imagePlayer)
        val name = itemView.findViewById<TextView>(R.id.playerName5)
        val sendButton = itemView.findViewById<Button>(R.id.sendRequest1)
    }
}
