package com.example.myapplication

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
import com.example.myapplication.wallpaper.ApplyWallpaper

class AllWallpaperAdapter(
    private val context: Context,
    private val wallpaperList: List<AllWallpaperDetailItem>
) : RecyclerView.Adapter<AllWallpaperAdapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.data_layer_wallpaper_detail, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = wallpaperList[position]
        Glide.with(context).load(item.imageUrl).error(R.drawable.baseline_error_outline_24).into(holder.wallpaperImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ApplyWallpaper::class.java).apply {
                putExtra( ApplyWallpaper.APPLY_WALLPAPER, item.imageUrl)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = wallpaperList.size

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wallpaperImage: ImageView = itemView.findViewById(R.id.wall_cover)
//        val wallpaperTitle: TextView = itemView.findViewById(R.id.wall_cover_title)
    }
}
