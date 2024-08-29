package com.example.myapplication.live_wallpapers

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityApplyLiveWallpaperBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ApplyLiveWallpaper : AppCompatActivity() {

    private lateinit var binding: ActivityApplyLiveWallpaperBinding
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private lateinit var myWallpaper: VideoView
    private lateinit var videoUri: Uri
    private var downloadID: Long = 0L
    private var downloadedFileName: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var currentVideoPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyLiveWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)

        myWallpaper = findViewById(R.id.to_apply_wallpaper)

        intent?.let {
            showSpinner()
            myWallpaper.setMediaController(null)

            myWallpaper.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                hideSpinner()
            }

            myWallpaper.setOnErrorListener { mp, what, extra ->
                Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
                hideSpinner()
                true
            }


            val applyingWallpaper = it.getStringExtra(APPLY_WALLPAPER)
            videoUri = Uri.parse(applyingWallpaper)
            myWallpaper.setVideoURI(videoUri)
            myWallpaper.requestFocus()
            myWallpaper.start()

            binding.backToWallpapers.setOnClickListener {
                finish()
            }
            binding.applyWallpaperOn.setOnClickListener {
                binding.applyWallpaperOn.isEnabled = false
                binding.applyWallpaperOn.text = "Starting download..."
                downloadedFileName = "live_wallpaper_${System.currentTimeMillis()}.mp4"
                download(videoUri, downloadedFileName)
            }
        } ?: run {
            finish()
        }

        // Register receiver to listen for the download completion
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
    private fun showSpinner() {
        bindingForLoading.spinner.visibility = View.VISIBLE
        bindingForLoading.overlay.visibility = View.VISIBLE
    }

    private fun hideSpinner() {
        bindingForLoading.spinner.visibility = View.GONE
        bindingForLoading.overlay.visibility = View.GONE
    }

    private fun download(url: Uri, fileName: String) {
        try {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(url)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                .setMimeType("video/mp4")
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle("Downloading wallpaper")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, File.separator + fileName)

            // Start download and save download ID
            downloadID = downloadManager.enqueue(request)
//            Toast.makeText(this, "Downloading wallpaper...", Toast.LENGTH_SHORT).show()

            // Start checking progress
            handler.postDelayed(checkDownloadProgress, 1000)
        } catch (e: Exception) {
            binding.applyWallpaperOn.isEnabled = true
            binding.applyWallpaperOn.text = getString(R.string.download)
            Toast.makeText(this, "Failed to download wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Runnable to check download progress
    private val checkDownloadProgress: Runnable = object : Runnable {
        @SuppressLint("Range")
        override fun run() {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadID)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val bytesDownloaded =
                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes =
                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                if (totalBytes > 0) {
                    val progress = (bytesDownloaded * 100L) / totalBytes
                    binding.applyWallpaperOn.text = "Downloading... $progress%"
                }

                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    binding.applyWallpaperOn.text = "Apply Wallpaper"
                    binding.applyWallpaperOn.isEnabled = true
                    return  // Stop the handler
                }
            }
            cursor.close()

            // Continue checking progress
            handler.postDelayed(this, 1000)
        }
    }

    // BroadcastReceiver to handle download completion
    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadID) {
                Toast.makeText(context, "Wallpaper downloaded successfully", Toast.LENGTH_SHORT)
                    .show()
                handler.removeCallbacks(checkDownloadProgress)
                binding.applyWallpaperOn.text = "Apply to Lock Screen"
                binding.applyWallpaperOn.isEnabled = true
                binding.applyWallpaperOn.setOnClickListener {
                    checkAndRequestPermissions()
                }
            }
        }
    }



    private fun applyWallpaperToLockScreen() {
        lifecycleScope.launch {
            showSpinner()
            binding.applyWallpaperOn.isEnabled = false
            binding.applyWallpaperOn.text = "Applying..."

            try {
                withContext(Dispatchers.IO) {
                    // Save the video path in shared preferences
                    val sharedPreferences = getSharedPreferences("live_wallpaper_prefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("video_path", videoUri.toString())
                        apply()
                    }

                    // Create an intent to launch the live wallpaper preview
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(this@ApplyLiveWallpaper, VideoWallpaperService::class.java)
                        )
                    }

                    // Launch the intent
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
                // Permission granted, proceed with applying wallpaper
                applyWallpaperToLockScreen()
            } else {
                Toast.makeText(this, "Permission denied. Unable to apply wallpaper.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        currentVideoPosition = myWallpaper.currentPosition
        myWallpaper.pause()
    }

    override fun onResume() {
        super.onResume()
        myWallpaper.seekTo(currentVideoPosition)
        myWallpaper.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
        handler.removeCallbacks(checkDownloadProgress)
    }

    companion object {
        const val APPLY_WALLPAPER = "apply_wallpaper"
        private val STORAGE_PERMISSION_CODE = 1001
    }
}