package com.example.jeudifferences

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatGameAdapter(val context: Context, val messageArray: ArrayList<message>, val currentUserName: String, val timeArray:ArrayList<String> ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val received = 1
    val sent = 2
    val globalMessage = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        if(viewType != 0 ) {
            if(viewType == 2) {
                val view = LayoutInflater.from(context).inflate(R.layout.sent_message, parent, false)
                return sentMessageView(view)
            }
            if (viewType == 3)  {
                val view = LayoutInflater.from(context).inflate(R.layout.global_message, parent, false)
                return globalMessageView(view)
            }
            if(viewType == 1) {
                val view = LayoutInflater.from(context).inflate(R.layout.received_text, parent, false)
                return receivedMessageView(view)
            }
        }

        val view = LayoutInflater.from(context).inflate(R.layout.received_text, parent, false)
        return receivedMessageView(view)

    }

    override fun getItemCount(): Int {
        return messageArray.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageArray[position]
        val currentTime = timeArray[position]

        if (holder.javaClass == globalMessageView::class.java) {
            val viewHolder = holder as  globalMessageView
            if(currentMessage.isGlobal == true && currentMessage.isKicked == true) {
                holder.globalMessage.text = currentMessage.message
                holder.timeGlobal.text = currentTime
            }

            if(currentMessage.isDeserter == true && currentMessage.isGlobal == true) {
                holder.globalMessage.text = currentMessage.message + " a abandonné la partie"
                holder.timeGlobal.text = currentTime
            }
            if (currentMessage.isCheating == true && currentMessage.isKicked == false) {
                holder.globalMessage.text = currentMessage.message + " a triché"
                holder.timeGlobal.text = currentTime
            }

            //erreur par ennemi
            if(currentMessage.isClickEnemy == true && currentMessage.isValid == false && currentMessage.isGlobal == true ) {
                holder.globalMessage.text = currentMessage.message + currentMessage.senderName
                holder.timeGlobal.text = currentTime
            }
            if (currentMessage.isClickEnemy == true && currentMessage.isValid == true && currentMessage.isGlobal == true ) {
                holder.globalMessage.text = currentMessage.message + currentMessage.senderName
                holder.timeGlobal.text = currentTime
            }
        }

        if(holder.javaClass == sentMessageView::class.java) {
            val viewHolder = holder as  sentMessageView

            if (currentMessage.isGlobal == false && currentMessage.isDeserter == false && currentMessage.isCheating == false){
                holder.sentMessage.text = "Vous: " + currentMessage.message
                holder.timeSent.text = currentTime
            }

        }

        if (holder.javaClass == receivedMessageView::class.java){
            val viewHolder = holder as  receivedMessageView

            if (currentMessage.isGlobal == false && currentMessage.isDeserter == false && currentMessage.isCheating == false){
                holder.receivedMessage.text = currentMessage.senderName +": " + currentMessage.message
                holder.timeReceived.text = currentTime
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageArray[position]
        if (currentMessage.isGlobal == false) {
            if(currentMessage.senderName == currentUserName) {
                return sent
            }
            else {
                return received
            }
        }
        else {
            return globalMessage
        }
    }

    fun clearMessages() {
        messageArray.clear()
        timeArray.clear()
        notifyDataSetChanged()
    }

    class sentMessageView (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.messageSent)
        val timeSent = itemView.findViewById<TextView>(R.id.MessageSentTime)
    }

    class receivedMessageView (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receivedMessage = itemView.findViewById<TextView>(R.id.messageReceived)
        val timeReceived = itemView.findViewById<TextView>(R.id.MessageReceivedTime)
    }

    class globalMessageView (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val globalMessage = itemView.findViewById<TextView>(R.id.globalMessage)
        val timeGlobal = itemView.findViewById<TextView>(R.id.globalTime)
    }

}

