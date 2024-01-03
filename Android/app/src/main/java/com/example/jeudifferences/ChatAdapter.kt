package com.example.jeudifferences

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import android.media.MediaPlayer
import com.bumptech.glide.Glide


//ce code est inspiré par une vidéo youtube : https://youtu.be/8Pv96bvBJL4?si=68Na0dVI-yhLk7Bs
class ChatAdapter(val context:Context, val messageArray: ArrayList<principalChatMessage>, val currentUserName: String, val timeArray:ArrayList<String> ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val received = 1
    val sent = 2


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        if(viewType == 2) {
            val view = LayoutInflater.from(context).inflate(R.layout.sent_message, parent, false)
            return sentMessageView(view)
        }
        else {
            val view = LayoutInflater.from(context).inflate(R.layout.received_text, parent, false)
            return receivedMessageView(view)
        }

    }

    override fun getItemCount(): Int {
       return messageArray.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageArray[position]
        val currentTime = timeArray[position]

        when (holder) {
            is sentMessageView -> {
                if (currentMessage.type == "voice") {
                    holder.playButton.visibility = View.VISIBLE
                    holder.sentMessage.text = "Message Vocal"
                    holder.itemView.setOnClickListener {
                        var url = "http://ec2-3-99-247-102.ca-central-1.compute.amazonaws.com:3000/api/users/voice-message/"
                        playVoiceMessage(url+currentMessage.message)
                    }
                } else {
                    holder.playButton.visibility = View.GONE
                    holder.sentMessage.text = "${currentMessage.message}"
                }
                holder.timeSent.text = currentTime
            }
            is receivedMessageView -> {
                if (currentMessage.type == "voice") {
                    holder.playButton.visibility = View.VISIBLE
                    holder.receivedMessage.text = "Message Vocal de ${currentMessage.sender} "
                    holder.itemView.setOnClickListener {
                        //Log.i("asdf","trying to listen")
                        var url = "http://ec2-3-99-247-102.ca-central-1.compute.amazonaws.com:3000/api/users/voice-message/"
                        playVoiceMessage(url+currentMessage.message)
                    }

                } else {
                    holder.playButton.visibility = View.GONE
                    holder.receivedMessage.text = "${currentMessage.message}"
                    //I have currentMessage.imageURL set the profile pic from the url
                    var imageUrl = "http://ec2-3-99-247-102.ca-central-1.compute.amazonaws.com:3000/api/users/get-public-profile-image/" + currentMessage.userId
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(context)
                            .load(imageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.profile_picture) // Add a placeholder image
                            .error(R.drawable.profile_picture)
                            .into(holder.profilePicture)
                    }
                }
                holder.timeReceived.text = currentTime
            }
        }
    }
    private fun playVoiceMessage(url: String) {
        val mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener { release() }
            } catch (e: IOException) {
                Log.e("ChatAdapter", "Error setting data source", e)
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageArray[position]
        if(currentMessage.sender == currentUserName) {
            return sent
        }
        else {
            return received
        }
    }
    class sentMessageView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.messageSent)
        val timeSent = itemView.findViewById<TextView>(R.id.MessageSentTime)
        val playButton = itemView.findViewById<ImageView>(R.id.sentPlayButton)  // Play button reference
    }

    class receivedMessageView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receivedMessage = itemView.findViewById<TextView>(R.id.messageReceived)
        val timeReceived = itemView.findViewById<TextView>(R.id.MessageReceivedTime)
        val playButton = itemView.findViewById<ImageView>(R.id.receivedPlayButton)  // Play button reference
        val profilePicture = itemView.findViewById<ImageView>(R.id.profilePicture)
    }

}
