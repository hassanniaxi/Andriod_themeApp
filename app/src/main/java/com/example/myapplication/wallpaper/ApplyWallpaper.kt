package com.example.myapplication.wallpaper

import android.app.Dialog
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityApplyWallpaperBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import kotlinx.coroutines.*


class ApplyWallpaper : AppCompatActivity() {

    private lateinit var binding: ActivityApplyWallpaperBinding
    private lateinit var wallpaperUri: Uri
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)

        intent?.let {
            val applyingWallpaper = it.getStringExtra(APPLY_WALLPAPER)
            applyingWallpaper?.let { imageUrl ->
                wallpaperUri = Uri.parse("android.resource://${applicationContext.packageName}/${imageUrl}")

                Glide.with(this)
                    .load(wallpaperUri)
                    .placeholder(R.drawable.wallicon)
                    .error(R.drawable.baseline_error_outline_24)
                    .into(binding.toApplyWallpaper)
            }

            binding.backToWallpapers.setOnClickListener {
                finish()
            }
            binding.applyWallpaperOn.setOnClickListener {
                    showBottomDialog()
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

    private fun showBottomDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.bottom_sheet_dialog_apply_wallpaper)
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                attributes.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.BOTTOM)
            }
        }

        val homeScreen = dialog.findViewById<LinearLayout>(R.id.setOnHomeScreen)
        val lockScreen = dialog.findViewById<LinearLayout>(R.id.setOnLockScreen)
        val homeAndLockScreens = dialog.findViewById<LinearLayout>(R.id.setOnHomeAndLockScreen)
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)

        homeScreen.setOnClickListener {
            applyWallpaper(WallpaperManager.FLAG_SYSTEM)
            dialog.dismiss()
        }

        lockScreen.setOnClickListener {
            applyWallpaper(WallpaperManager.FLAG_LOCK)
            dialog.dismiss()
        }

        homeAndLockScreens.setOnClickListener {
            applyWallpaper( WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }




    private fun applyWallpaper(flags: Int) {
        showSpinner()

        val wallpaperManager = WallpaperManager.getInstance(this@ApplyWallpaper)

        Glide.with(this)
            .asBitmap()
            .load(wallpaperUri)
            .override(1080, 1920)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            when (flags) {
                                WallpaperManager.FLAG_SYSTEM -> {
                                    wallpaperManager.setBitmap(resource)
                                }
                                WallpaperManager.FLAG_LOCK -> {
                                    wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK)
                                }
                                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK -> {
                                    wallpaperManager.setBitmap(resource)
                                    wallpaperManager.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ApplyWallpaper, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ApplyWallpaper, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                hideSpinner()
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
