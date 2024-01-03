package com.example.jeudifferences

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jeudifferences.databinding.UserItemBinding
import org.json.JSONObject

class ActivePlayerCarousel(): RecyclerView.Adapter<ActivePlayerCarousel.ViewHolder>() {

    private lateinit var usersList: List<Users>
    private lateinit var context: Context

    constructor(usersList: List<Users>, context: Context) : this() {
        this.usersList = usersList
        this.context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: ActivePlayerCarousel.ViewHolder, position: Int) {
        val user = this.usersList[position]
        holder.username.text = user.username
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.usernameItem)
    }

}
