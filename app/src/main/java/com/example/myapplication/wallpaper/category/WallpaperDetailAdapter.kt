package com.example.myapplication.wallpaper.category

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.wallpaper.ApplyWallpaper
import com.example.myapplication.wallpaper.WallpaperDetailItems

class WallpaperDetailAdapter(
    private val context: Context,
    private val wallpaperDetailList: List<WallpaperDetailItems>
) : RecyclerView.Adapter<WallpaperDetailAdapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.data_layer_wallpaper_detail, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = wallpaperDetailList[position]
        Glide.with(context).load(item.imageUrl.toInt()).placeholder(R.drawable.wallicon).error(com.example.myapplication.R.drawable.baseline_error_outline_24).into(holder.imageView)


        holder.itemView.setOnClickListener {
            val intent = Intent(context, ApplyWallpaper::class.java).apply {
                putExtra(ApplyWallpaper.APPLY_WALLPAPER, item.imageUrl)
            }
            context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int = wallpaperDetailList.size

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.wall_cover)
    }
}