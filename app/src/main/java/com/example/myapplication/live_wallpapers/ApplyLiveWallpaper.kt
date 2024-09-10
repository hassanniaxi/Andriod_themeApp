package com.example.myapplication.live_wallpapers

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplication.databinding.ActivityApplyLiveWallpaperBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplyLiveWallpaper : AppCompatActivity() {

    private lateinit var binding: ActivityApplyLiveWallpaperBinding
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private lateinit var playerView: PlayerView
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var videoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyLiveWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)

        playerView = binding.toApplyWallpaper
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer

        intent?.let {
            showSpinner()

            val applyingWallpaper = it.getStringExtra(APPLY_LIVE_WALLPAPER)
            videoUri = Uri.parse("android.resource://${applicationContext.packageName}/${applyingWallpaper}")

            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        hideSpinner()
                    }
                }
            })

            exoPlayer.repeatMode = Player.REPEAT_MODE_ALL

            binding.backToWallpapers.setOnClickListener {
                finish()
            }

            binding.applyWallpaperOn.setOnClickListener {
                    checkAndRequestPermissions()
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

    private fun applyWallpaperToLockScreen() {
        lifecycleScope.launch {
            showSpinner()
            binding.applyWallpaperOn.isEnabled = false
            binding.applyWallpaperOn.text = "Applying..."

            try {
                withContext(Dispatchers.IO) {

                    VideoWallpaperService.videoUri = videoUri

                    WallpaperManager.getInstance(this@ApplyLiveWallpaper).apply {
                        clear()
                    }

                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(this@ApplyLiveWallpaper, VideoWallpaperService::class.java)
                        )
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ApplyLiveWallpaper, "Failed to apply wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                hideSpinner()
                binding.applyWallpaperOn.isEnabled = false
                binding.applyWallpaperOn.text = "Applied to Lock Screen"
            }
        }
    }


    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val notGrantedPermissions = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            if (notGrantedPermissions.isNotEmpty()) {
                requestPermissions(notGrantedPermissions.toTypedArray(), STORAGE_PERMISSION_CODE)
            } else {
                applyWallpaperToLockScreen()
            }
        } else {
            applyWallpaperToLockScreen()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                applyWallpaperToLockScreen()
            } else {
                Toast.makeText(this, "Permission denied. Unable to apply wallpaper.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val APPLY_LIVE_WALLPAPER = "apply_love_wallpaper"
        val STORAGE_PERMISSION_CODE = 1001
    }
}