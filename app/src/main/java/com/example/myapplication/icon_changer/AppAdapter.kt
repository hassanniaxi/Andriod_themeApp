package com.example.myapplication.icon_changer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.NavigationHandler
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemAppBinding
import com.example.myapplication.ringtone.Ringtone
import com.example.walltone.ringtone.RingtoneAdapter
import com.example.walltone.ringtone.RingtoneAdapter.Companion
import kotlin.math.abs

class AppAdapter(
    private var appList: List<AppInfo>,
    private val context: Context,
    private val navController: NavController
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f

    companion object {
        const val MINI_DISTANCE = 50
    }

    inner class AppViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: AppInfo) {
            binding.appNameTextView.text = appInfo.appName
            binding.appIconImageView.setImageDrawable(appInfo.appIcon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(appList[position])

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MakeManipulation::class.java).apply {
                putExtra(MakeManipulation.APP_POS, position)
                putExtra(MakeManipulation.APP_LOGO, appList[position].appIcon?.toBitmap())
                putParcelableArrayListExtra(
                    MakeManipulation.APP_INFO_LIST,
                    ArrayList(appList)
                )
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

    fun updateList(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }

    fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return this.bitmap
        }
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    override fun getItemCount(): Int = appList.size

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
            if (deltaX > 0) {
                navController.let { NavigationHandler.navigateToDestination(it, R.id.wallpapers) }
            }else{
                navController.let { NavigationHandler.navigateToDestination(it, R.id.ringtones) }
            }
        }
    }
}
