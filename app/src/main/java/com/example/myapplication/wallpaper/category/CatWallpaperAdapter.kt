package com.example.myapplication.wallpaper.category

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.NavigationHandler
import kotlin.math.abs

class CatWallpaperAdapter(
    private val context: Context,
    private val wallpaperList: List<CatWallpaperItem>,
    private val navController: NavController
) : RecyclerView.Adapter<CatWallpaperAdapter.MyHolder>() {

    private var x1: Float = 0.0f
    private var y1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y2: Float = 0.0f

    companion object {
        private const val MINI_DISTANCE = 50
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.data_layer, parent, false)
        return MyHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = wallpaperList[position]
        holder.wallpaperTitle.text = item.title

        Glide.with(context).load(item.imageUrl.toInt()).placeholder(R.drawable.wallicon).error(R.drawable.baseline_error_outline_24).into(holder.wallpaperImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, WallpaperDetailActivity::class.java).apply {
                putExtra(WallpaperDetailActivity.EXTRA_WALLPAPER_TITLE, item.title)
            }
            context.startActivity(intent)
        }

        holder.itemView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            false
        }
    }

    override fun getItemCount(): Int = wallpaperList.size

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wallpaperImage: ImageView = itemView.findViewById(R.id.wall_cover)
        val wallpaperTitle: TextView = itemView.findViewById(R.id.wall_cover_title)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            x1 = e.x
            y1 = e.y
            return true
        }
    })

    private fun handleSwipeGesture(event: MotionEvent) {
        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) > MINI_DISTANCE && abs(deltaY) < MINI_DISTANCE) {
            if (deltaX < 0) {
                navController.let { NavigationHandler.navigateToDestination(it, R.id.icon_changer) }
            }
        }
    }
}
