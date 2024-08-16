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
import com.example.myapplication.R

class WallpaperAdapter(
    private val context: Context,
    private val wallpaperList: List<WallpaperItem>
) : RecyclerView.Adapter<WallpaperAdapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.data_layer, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = wallpaperList[position]
        holder.wallpaperTitle.text = item.title
        Glide.with(context).load(item.imageUrl).error(R.drawable.baseline_error_outline_24).into(holder.wallpaperImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, WallpaperDetailActivity::class.java).apply {
                putExtra(WallpaperDetailActivity.EXTRA_WALLPAPER_TITLE, item.title)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = wallpaperList.size

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wallpaperImage: ImageView = itemView.findViewById(R.id.wall_cover)
        val wallpaperTitle: TextView = itemView.findViewById(R.id.wall_cover_title)
    }
}
