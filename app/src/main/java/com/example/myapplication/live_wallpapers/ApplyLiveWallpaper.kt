package com.example.myapplication.live_wallpapers

import android.app.Dialog
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityApplyLiveWallpaperBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import kotlinx.coroutines.*

class ApplyLiveWallpaper : AppCompatActivity() {

    private lateinit var binding: ActivityApplyLiveWallpaperBinding
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private lateinit var myWallpaper: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyLiveWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)
        myWallpaper = findViewById(R.id.to_apply_wallpaper)

        // Show spinner while preparing video
        showSpinner()

        myWallpaper.setMediaController(null)

        // Hide spinner when video is prepared and ready to play
        myWallpaper.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            hideSpinner()  // Hide spinner when video is ready
        }

        intent?.let {
            val applyingWallpaper = it.getStringExtra(APPLY_WALLPAPER)
            val wall = Uri.parse(applyingWallpaper)
            myWallpaper.setVideoURI(wall)
            myWallpaper.requestFocus()
            myWallpaper.start()

            binding.backToWallpapers.setOnClickListener {
                finish()
            }
            binding.applyWallpaperOn.setOnClickListener {
                if (applyingWallpaper != null) {
                    showBottomDialog(applyingWallpaper)
                }
            }
        } ?: run {
            finish()
        }
    }

    private fun showSpinner() {
        bindingForLoading.spinner.visibility = View.VISIBLE
        bindingForLoading.overlay.visibility = View.VISIBLE
    }

    private fun hideSpinner() {
        bindingForLoading.spinner.visibility = View.GONE
        bindingForLoading.overlay.visibility = View.GONE
    }

    private fun showBottomDialog(imageUrl: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_dialog_apply_wallpaper)

        val homeScreen = dialog.findViewById<LinearLayout>(R.id.setOnHomeScreen)
        val lockScreen = dialog.findViewById<LinearLayout>(R.id.setOnLockScreen)
        val homeAndLockScreens = dialog.findViewById<LinearLayout>(R.id.setOnHomeAndLockScreen)
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)

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
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun applyWallpaper(imageUrl: String, selectedOption: Int) {
        showSpinner()

        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val wallpaperManager = WallpaperManager.getInstance(this@ApplyLiveWallpaper)
                        try {
                            when (selectedOption) {
                                0 -> wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_SYSTEM) // Home screen
                                1 -> wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK) // Lock screen
                                2 -> {
                                    wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_SYSTEM) // Home screen
                                    wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK) // Lock screen
                                }
                                else -> withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ApplyLiveWallpaper, "No option selected", Toast.LENGTH_SHORT).show()
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ApplyLiveWallpaper, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
                                hideSpinner() // Ensure this is on the main thread
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ApplyLiveWallpaper, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                                hideSpinner() // Ensure this is on the main thread
                            }
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle placeholder if needed
                }
            })
    }

    companion object {
        const val APPLY_WALLPAPER = "apply_wallpaper"
    }
}
