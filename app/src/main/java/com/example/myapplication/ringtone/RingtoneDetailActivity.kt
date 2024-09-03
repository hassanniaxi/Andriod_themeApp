package com.example.myapplication.ringtone

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityRingtoneDetailBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import kotlinx.coroutines.*

class RingtoneDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRingtoneDetailBinding
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION
    private lateinit var ringtoneList: List<RingtoneItem>
    private var duration: Int = 0
    private var bottomDialog: Dialog? = null
    private var downloadID: Long = 0L
    private lateinit var ringtoneUri: Uri
    private var downloadedFileName: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var downloadManager: DownloadManager
    private var downloadedRingtoneUri: Uri? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var savedPosition: Int = RecyclerView.NO_POSITION

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                val currentPosition = it.currentPosition
                if (binding.completionLine.progress != currentPosition) {
                    binding.completionLine.progress = currentPosition
                    binding.ringtonePlayDuration.text = formatDuration(currentPosition)
                }
                updateHandler.postDelayed(this, 200)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRingtoneDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        sharedPreferences = getSharedPreferences("ringtone_prefs", Context.MODE_PRIVATE)

        intent?.let { intent ->
            currentlyPlayingPosition = intent.getIntExtra(EXTRA_PLAYING_POSITION, RecyclerView.NO_POSITION)
            ringtoneList = intent.getParcelableArrayListExtra(EXTRA_RINGTONE_LIST) ?: listOf()
            ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
                binding.fullDurationTime.text = "Loading..."
                binding.ringtoneTitleTextView.text = ringtone.title
                binding.ringtoneAuthorTextView.text = ringtone.author
                ringtoneUri = Uri.parse(ringtone.resourceId)

                CoroutineScope(Dispatchers.IO).launch {
                    val fetchedDuration = getRingtoneDuration(ringtone.resourceId)
                    withContext(Dispatchers.Main) {
                        duration = fetchedDuration
                        binding.fullDurationTime.text = formatDuration(duration)
                    }
                }
            } ?: run {
                finish() // Finish activity if no valid ringtone
            }
        } ?: run {
            finish() // Finish activity if intent is null
        }

        if (savedInstanceState != null) {
            currentlyPlayingPosition = savedInstanceState.getInt("saved_position", RecyclerView.NO_POSITION)
            downloadedFileName = savedInstanceState.getString("downloaded_file_name", "")
            // Restore other relevant state data here
        }

        setupListeners()
        playCurrentRingtone()

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("saved_position", currentlyPlayingPosition)
        outState.putString("downloaded_file_name", downloadedFileName)
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.let { updateHandler.post(updateRunnable) }
    }

    override fun onPause() {
        super.onPause()
        pausePlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        updateHandler.removeCallbacks(updateRunnable)
        unregisterReceiver(onDownloadComplete)
        bottomDialog?.dismiss()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun getRingtoneDuration(resourceId: String): Int {
        return try {
            val tempMediaPlayer = MediaPlayer().apply {
                setDataSource(this@RingtoneDetailActivity, Uri.parse(resourceId))
                prepare()
            }
            val fetchedDuration = tempMediaPlayer.duration
            tempMediaPlayer.release()
            fetchedDuration
        } catch (e: Exception) {
            0
        }
    }

    private fun extractRingtoneTitleFromUri(uri: Uri): String {
        val path = uri.path
        return path?.substringAfterLast('/')?.substringBeforeLast('.') ?: "unknown"
    }

    private fun isRingtoneDownloaded(ringtoneTitle: String): Boolean {
        val downloadedRingtones = sharedPreferences.getStringSet("downloaded_ringtones", mutableSetOf()) ?: mutableSetOf()
        return downloadedRingtones.contains(ringtoneTitle)
    }

    private fun setupListeners() {
        val ringtoneTitle = extractRingtoneTitleFromUri(ringtoneUri)
        binding.applyRingtoneOn.text = if (isRingtoneDownloaded(ringtoneTitle)) {
            "Apply Ringtone"
        } else {
            "Download"
        }
        binding.applyRingtoneOn.isEnabled = !isRingtoneDownloaded(ringtoneTitle)

        binding.backToRingtones.setOnClickListener {
            stopPlayback()
            finish()
        }

        binding.applyRingtoneOn.setOnClickListener {
            if (!isRingtoneDownloaded(ringtoneTitle)) {
                checkAndRequestDownloadPermission()
            } else {
                showRingtoneBottomDialog()
            }
        }

        binding.playPauseRingtone.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pausePlayback() else resumePlayback()
        }

        binding.previousRingtone.setOnClickListener {
            playPreviousRingtone()
        }

        binding.nextRingtone.setOnClickListener {
            playNextRingtone()
        }
    }

    private fun proceedWithDownload() {
        binding.applyRingtoneOn.isEnabled = false
        binding.applyRingtoneOn.text = "Starting download..."
        val ringtoneTitle = extractRingtoneTitleFromUri(ringtoneUri)
        downloadedFileName = "ringtone_${ringtoneTitle}.mp3"
        downloadRingtone(ringtoneUri, downloadedFileName)
    }

    private fun downloadRingtone(url: Uri, fileName: String) {
        try {
            val request = DownloadManager.Request(url)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                .setMimeType("audio/mpeg")
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle("Downloading Ringtone")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_RINGTONES, fileName)

            downloadID = downloadManager.enqueue(request)
            markRingtoneAsDownloaded(extractRingtoneTitleFromUri(url))

            handler.postDelayed(checkDownloadProgress, 1000)
        } catch (e: Exception) {
            binding.applyRingtoneOn.isEnabled = true
            binding.applyRingtoneOn.text = "Download"
            Toast.makeText(this, "Failed to download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val checkDownloadProgress: Runnable = object : Runnable {
        @SuppressLint("Range")
        override fun run() {
            val query = DownloadManager.Query().setFilterById(downloadID)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (totalBytes > 0) {
                    val progress = (bytesDownloaded * 100L) / totalBytes
                    binding.applyRingtoneOn.text = "Downloading... $progress%"
                }

                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    binding.applyRingtoneOn.text = "Apply"
                    binding.applyRingtoneOn.isEnabled = true
                    return
                }
            }
            cursor.close()

            handler.postDelayed(this, 1000)
        }
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadID) {
                val query = DownloadManager.Query().setFilterById(downloadID)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                    downloadedRingtoneUri = Uri.parse(fileUri)
                    cursor.close()

                    Toast.makeText(context, "Ringtone downloaded successfully", Toast.LENGTH_SHORT).show()
                    binding.applyRingtoneOn.text = "Apply"
                    binding.applyRingtoneOn.isEnabled = true
                }
            }
        }
    }

    private fun markRingtoneAsDownloaded(ringtoneTitle: String) {
        val downloadedRingtones = sharedPreferences.getStringSet("downloaded_ringtones", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        downloadedRingtones.add(ringtoneTitle)
        sharedPreferences.edit().putStringSet("downloaded_ringtones", downloadedRingtones).apply()
    }

    private fun showRingtoneBottomDialog() {
        bottomDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.bottom_sheet_dialog_apply_ringtone)
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                attributes?.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.BOTTOM)
            }
        }

        bottomDialog?.findViewById<LinearLayout>(R.id.setOnPhone)?.setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_RINGTONE)
            bottomDialog?.dismiss()
        }

        bottomDialog?.findViewById<LinearLayout>(R.id.setOnAlarm)?.setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_ALARM)
            bottomDialog?.dismiss()
        }

        bottomDialog?.findViewById<LinearLayout>(R.id.setOnNotification)?.setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_NOTIFICATION)
            bottomDialog?.dismiss()
        }

        bottomDialog?.findViewById<ImageView>(R.id.cancelButton)?.setOnClickListener {
            bottomDialog?.dismiss()
        }

        bottomDialog?.show()
    }

    private fun applyRingtone(type: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            requestWriteSettingsPermission()
            return
        }

        downloadedRingtoneUri?.let { uri ->
            lifecycleScope.launch {
                showSpinner()
                binding.applyRingtoneOn.isEnabled = false
                binding.applyRingtoneOn.text = "Applying..."

                try {
                    withContext(Dispatchers.IO) {
                        RingtoneManager.setActualDefaultRingtoneUri(this@RingtoneDetailActivity, type, uri)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RingtoneDetailActivity, "Ringtone applied successfully", Toast.LENGTH_SHORT).show()
                        binding.applyRingtoneOn.text = "Applied"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RingtoneDetailActivity, "Failed to apply ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.applyRingtoneOn.text = "Apply"
                    }
                } finally {
                    binding.applyRingtoneOn.isEnabled = true
                    hideSpinner()
                }
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        binding.playPauseRingtone.setImageResource(R.drawable.baseline_play_arrow_24)
    }

    private fun playPreviousRingtone() {
        currentlyPlayingPosition = (currentlyPlayingPosition - 1).takeIf { it >= 0 } ?: ringtoneList.size - 1
        updateUIForCurrentRingtone()
    }

    private fun playNextRingtone() {
        currentlyPlayingPosition = (currentlyPlayingPosition + 1) % ringtoneList.size
        updateUIForCurrentRingtone()
    }

    private fun formatDuration(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showSpinner() {
        bindingForLoading.root.visibility = View.VISIBLE
    }

    private fun hideSpinner() {
        bindingForLoading.root.visibility = View.GONE
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.playPauseRingtone.setImageResource(
            if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
        )
    }

    private fun handlePlaybackState(isPlaying: Boolean) {
        if (isPlaying) resumePlayback() else pausePlayback()
        updatePlayPauseButton(isPlaying)
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        updatePlayPauseButton(false)
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        updatePlayPauseButton(true)
        updateHandler.post(updateRunnable)
    }

    private fun updateUIForCurrentRingtone() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            binding.ringtoneTitleTextView.text = ringtone.title
            binding.ringtoneAuthorTextView.text = ringtone.author
            binding.fullDurationTime.text = "Loading..."
            binding.completionLine.progress = 0

            // Stop any ongoing playback
            stopPlayback()

            // Play the new ringtone
            playRingtone(ringtone.resourceId)
        }
    }

    private fun playCurrentRingtone() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            ringtoneUri = Uri.parse(ringtone.resourceId)
            binding.ringtoneTitleTextView.text = ringtone.title
            binding.ringtoneAuthorTextView.text = ringtone.author
            binding.fullDurationTime.text = "Loading..."
            binding.completionLine.progress = 0

            // Play the new ringtone
            playRingtone(ringtone.resourceId)
        }
    }

    private fun playRingtone(resourceId: String) {
        try {
            mediaPlayer?.apply {
                reset()
                setDataSource(applicationContext, Uri.parse(resourceId))
                setOnPreparedListener {
                    start()
                    binding.playPauseRingtone.setImageResource(R.drawable.baseline_pause_24)
                    binding.completionLine.max = it.duration
                    binding.fullDurationTime.text = formatDuration(it.duration)
                    updateHandler.post(updateRunnable)
                }
                prepareAsync()
            } ?: run {
                mediaPlayer = MediaPlayer().apply {
                    setOnCompletionListener {
                        mediaPlayer?.reset()
                        binding.playPauseRingtone.setImageResource(R.drawable.baseline_play_arrow_24)
                        updateHandler.removeCallbacks(updateRunnable)
                        playNextRingtone()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${packageName}")
                startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
            } else {
                showRingtoneBottomDialog()
            }
        } else {
            showRingtoneBottomDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                showRingtoneBottomDialog()
            } else {
                Toast.makeText(this, "Permission required to apply ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                proceedWithDownload()
            } else {
                Toast.makeText(this, "Permission denied. Unable to download ringtone.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkAndRequestDownloadPermission() {
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
                proceedWithDownload()
            }
        } else {
            proceedWithDownload()
        }
    }

    companion object {
        private const val REQUEST_CODE_WRITE_SETTINGS = 200
        const val STORAGE_PERMISSION_CODE = 1001
        const val EXTRA_RINGTONE_LIST = "extra_ringtone_list"
        const val EXTRA_PLAYING_POSITION = "extra_playing_position"
    }
}
