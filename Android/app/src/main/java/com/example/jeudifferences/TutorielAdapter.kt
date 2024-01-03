package com.example.jeudifferences

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jeudifferences.databinding.TutorielItemBinding

class TutorielAdapter(private val tutoImages: IntArray, val context: Context): RecyclerView.Adapter<TutorielAdapter.TutoViewHolder>() {
    class TutoViewHolder(val binding: TutorielItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TutoViewHolder {
        val binding =
            TutorielItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TutoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return tutoImages.size
    }

    override fun onBindViewHolder(holder: TutoViewHolder, position: Int) {
        var image = tutoImages[position]
        holder.binding.apply {
            Glide.with(imageTuto).load(image).into(imageTuto)
        }

    }
}
