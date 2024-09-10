package com.example.myapplication.live_wallpapers

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
            applyingWallpaper?.let { drawableName ->
                videoUri = Uri.parse("android.resource://${applicationContext.packageName}/$drawableName")

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
                Toast.makeText(this, "Error: No wallpaper data found.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "Error: Intent data is null.", Toast.LENGTH_SHORT).show()
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

    private fun applyLiveWallpaper() {
        lifecycleScope.launch {
            showSpinner()
            binding.applyWallpaperOn.isEnabled = false
            binding.applyWallpaperOn.text = "Applying..."

            try {
                withContext(Dispatchers.IO) {
                    VideoWallpaperService.videoUri = videoUri

                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(this@ApplyLiveWallpaper, VideoWallpaperService::class.java)
                        )
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ApplyLiveWallpaper, "Failed to apply wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                hideSpinner()
                binding.applyWallpaperOn.isEnabled = true
                binding.applyWallpaperOn.text = "Applied Successfully!!!"
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            val notGrantedPermissions = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            if (notGrantedPermissions.isNotEmpty()) {
                requestPermissions(notGrantedPermissions.toTypedArray(), STORAGE_PERMISSION_CODE)
            } else {
                applyLiveWallpaper()
            }
        } else {
            applyLiveWallpaper()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                applyLiveWallpaper()
            } else {
                Toast.makeText(this, "Permission denied. Unable to apply wallpaper.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    companion object {
        const val APPLY_LIVE_WALLPAPER = "apply_live_wallpaper"
        const val STORAGE_PERMISSION_CODE = 1001
    }
}
