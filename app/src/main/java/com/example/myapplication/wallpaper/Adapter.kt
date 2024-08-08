package com.example.myapplication.wallpaper

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R


class Adapter(
    private val context: Context,
    private val wallpaperList: MutableList<Pair<String, Pair<Int, Class<out AppCompatActivity>>>>
) : RecyclerView.Adapter<Adapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.data_layer, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val (title, imageResIdAndActivity) = wallpaperList[position]
        val (imageResId, activityClass) = imageResIdAndActivity

        holder.wallCoverTitle.text = title

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(imageResId)
            .placeholder(R.drawable.wallicon) // Placeholder while loading
            .error(R.drawable.baseline_front_hand_24) // Error image if loading fails
            .into(holder.wallCover)

        holder.itemView.setOnClickListener {
            try {
                val intent = Intent(context, activityClass)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                // Optionally show a toast or error message
            }
        }
    }

    override fun getItemCount(): Int = wallpaperList.size

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wallCover: ImageView = itemView.findViewById(R.id.wall_cover)
        val wallCoverTitle: TextView = itemView.findViewById(R.id.wall_cover_title)
    }
}
