package com.example.myapplication.wallpaper

import android.app.Dialog
import android.app.WallpaperManager
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityApplyWallpaperBinding
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.request.target.CustomTarget

class ApplyWallpaper : AppCompatActivity() {

    private lateinit var binding: ActivityApplyWallpaperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            val applyingWallpaper = it.getStringExtra(APPLY_WALLPAPER)
            applyingWallpaper?.let { imageUrl ->
                Glide.with(this)
                    .load(imageUrl)
                    .error(R.drawable.baseline_error_outline_24)
                    .into(binding.toApplyWallpaper)
            }

            binding.backToWallpapers.setOnClickListener{
                finish()
            }
            binding.applyWallpaperOn.setOnClickListener{
                if (applyingWallpaper != null) {
                    showBottomDialog(applyingWallpaper)
                }
            }
        } ?: run {
            finish()
        }
    }

    private fun showBottomDialog(imageUrl: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(com.example.myapplication.R.layout.bottom_sheet_dialog_apply_wallpaper)

        val homeScreen = dialog.findViewById<TextView>(com.example.myapplication.R.id.setOnHomeScreen)
        val lockScreen = dialog.findViewById<TextView>(com.example.myapplication.R.id.setOnLockScreen)
        val homeAndLockScreens = dialog.findViewById<TextView>(com.example.myapplication.R.id.setOnHomeAndLockScreen)
        val cancelButton = dialog.findViewById<ImageView>(com.example.myapplication.R.id.cancelButton)

        homeScreen.setOnClickListener {
            applyWallpaper(imageUrl, 0)
            dialog.dismiss()
        }

        lockScreen.setOnClickListener {
            applyWallpaper(imageUrl, 1)
            dialog.dismiss()
        }

        homeAndLockScreens.setOnClickListener {
            applyWallpaper(imageUrl, 2)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = com.example.myapplication.R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun applyWallpaper(imageUrl: String, selectedOption: Int) {
        Glide.with(this).asBitmap().load(imageUrl).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                val wallpaperManager = WallpaperManager.getInstance(this@ApplyWallpaper)
                try {
                    when (selectedOption) {
                        0 -> wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_SYSTEM) // Home screen
                        1 -> wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK) // Lock screen
                        2 -> {
                            wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_SYSTEM) // Home screen
                            wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK) // Lock screen
                        }
                        else -> Toast.makeText(this@ApplyWallpaper, "No option selected", Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this@ApplyWallpaper, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@ApplyWallpaper, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // Handle placeholder if needed, or leave empty
            }
        })
    }

    companion object {
        const val APPLY_WALLPAPER = "apply_wallpaper"
    }
}
