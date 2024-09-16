package com.example.myapplication.icon_changer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemAppBinding

class AppAdapter(
    private var appList: List<AppInfo>,
    private val context: Context
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

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
}
