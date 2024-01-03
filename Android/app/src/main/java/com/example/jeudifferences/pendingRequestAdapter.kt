package com.example.jeudifferences

import SocketClientHandler
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson

class pendingRequestAdapter(val context: Context, val pendingArray: ArrayList<FriendRequest>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.pending_request_item, parent, false)
        return pendingRequestsView(view)
    }

    override fun getItemCount(): Int {
        return pendingArray.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = pendingArray[position]
        val clientSocket = SocketClientHandler
        val gson = Gson()
        clientSocket.connect()

        val viewHolder = holder as pendingRequestAdapter.pendingRequestsView
        Glide.with(context)
            .load("http://ec2-3-99-221-92.ca-central-1.compute.amazonaws.com:3000/api/users/get-public-profile-image/${currentMessage.from.userId}")
            .into(viewHolder.image)
        holder.name.text = currentMessage.from.username

        holder.acceptButton.setOnClickListener {
            val friendRequest = FriendRequest(currentMessage.from, currentMessage.to, "accepted")
            val payload = Payload(friendRequest)
            val jsonString = gson.toJson(payload)
            clientSocket.send("friend_request_response", jsonString)
            val position = pendingArray.indexOf(currentMessage)
            if (position != -1) {
                // Remove the item from the data set
                pendingArray.removeAt(position)

                // Notify the adapter about the removal
                notifyItemRemoved(position)
            }
        }
        holder.rejectButton.setOnClickListener {
            val friendRequest = FriendRequest(currentMessage.from, currentMessage.to, "declined")
            val payload = Payload(friendRequest)
            val jsonString = gson.toJson(payload)
            clientSocket.send("friend_request_response", jsonString)
            val position = pendingArray.indexOf(currentMessage)
            if (position != -1) {
                // Remove the item from the data set
                pendingArray.removeAt(position)

                // Notify the adapter about the removal
                notifyItemRemoved(position)
            }
        }
    }

    class pendingRequestsView (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.imagePendingPlayer)
        val name = itemView.findViewById<TextView>(R.id.playerName6)
        val rejectButton = itemView.findViewById<Button>(R.id.rejectRequest)
        val acceptButton = itemView.findViewById<Button>(R.id.acceptRequest)
    }
}
