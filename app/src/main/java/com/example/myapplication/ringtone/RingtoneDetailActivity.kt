package com.example.myapplication.ringtone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
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
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityRingtoneDetailBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class RingtoneDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRingtoneDetailBinding
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION
    private lateinit var ringtoneList: List<RingtoneItem>
    private var duration: Int = 0
    private var bottomDialog: Dialog? = null
    private lateinit var ringtoneUri: Uri

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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            showRingtoneBottomDialog()
        } else {
            Toast.makeText(this, "Permissions are required to apply ringtone", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRingtoneDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)

        intent?.let { intent ->
            currentlyPlayingPosition = intent.getIntExtra(EXTRA_PLAYING_POSITION, RecyclerView.NO_POSITION)
            ringtoneList = intent.getParcelableArrayListExtra(EXTRA_RINGTONE_LIST) ?: listOf()
            ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
                binding.fullDurationTime.text = "Loading..."
                binding.ringtoneTitleTextView.text = ringtone.title
                binding.ringtoneAuthorTextView.text = ringtone.author
                ringtoneUri = Uri.parse("android.resource://${applicationContext.packageName}/${ringtone.resourceId}")

                CoroutineScope(Dispatchers.IO).launch {
                    val fetchedDuration = getRingtoneDuration(ringtone.resourceId)
                    withContext(Dispatchers.Main) {
                        duration = fetchedDuration
                        binding.fullDurationTime.text = formatDuration(duration)
                    }
                }
            } ?: run {
                finish()
            }
        } ?: run {
            finish()
        }

        if (savedInstanceState != null) {
            currentlyPlayingPosition = savedInstanceState.getInt("saved_position", RecyclerView.NO_POSITION)
        }

        setupListeners()
        playCurrentRingtone()
        mediaPlayer?.setOnCompletionListener {
            onRingtonePlayCompleted()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("saved_position", currentlyPlayingPosition)
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

    private fun setupListeners() {
        binding.backToRingtones.setOnClickListener {
            stopPlayback()
            finish()
        }

        binding.applyRingtoneOn.setOnClickListener {
            showRingtoneBottomDialog()
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

    private fun onRingtonePlayCompleted() {
        updatePlayPauseButton(false)
        mediaPlayer?.apply {
            seekTo(0)
            start()
        }
        updatePlayPauseButton(true)
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
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val writeSettingsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
                val accessNotificationPolicyPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                val postNotificationsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PackageManager.PERMISSION_GRANTED
                }

                when {
                    writeSettingsPermission == PackageManager.PERMISSION_GRANTED &&
                            accessNotificationPolicyPermission == PackageManager.PERMISSION_GRANTED &&
                            postNotificationsPermission == PackageManager.PERMISSION_GRANTED -> {
                        performRingtoneSet(type)
                    }
                    else -> {
                        requestPermissionLauncher.launch(arrayOf(
                            Manifest.permission.WRITE_SETTINGS,
                            Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                            Manifest.permission.POST_NOTIFICATIONS
                        ))
                    }
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (Settings.System.canWrite(this)) {
                    performRingtoneSet(type)
                } else {
                    requestWriteSettingsPermission()
                }
            }
            else -> {
                performRingtoneSet(type)
            }
        }
    }


    private fun performRingtoneSet(type: Int) {
        try {
            // Assuming ringtoneList[currentlyPlayingPosition].resourceId is a String that holds the resource name.
            val resourceName = ringtoneList[currentlyPlayingPosition].resourceId
            val rawResourceId = resources.getIdentifier(resourceName, "raw", packageName)

            val values = ContentValues()
            val ringtoneType: Int
            val toastMSG: String

            when (type) {
                RingtoneManager.TYPE_RINGTONE -> {
                    toastMSG = "Ringtone is set"
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    ringtoneType = RingtoneManager.TYPE_RINGTONE
                }
                RingtoneManager.TYPE_NOTIFICATION -> {
                    toastMSG = "Notification Tone is set"
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                    ringtoneType = RingtoneManager.TYPE_NOTIFICATION
                }
                RingtoneManager.TYPE_ALARM -> {
                    toastMSG = "Alarm tone is set"
                    values.put(MediaStore.Audio.Media.IS_ALARM, true)
                    ringtoneType = RingtoneManager.TYPE_ALARM
                }
                else -> {
                    toastMSG = "Ringtone is set"
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    ringtoneType = RingtoneManager.TYPE_RINGTONE
                }
            }

            val ringtoneFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "$resourceName.mp3")
            val inputStream = resources.openRawResource(rawResourceId)
            val outputStream = FileOutputStream(ringtoneFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        output.write(buffer, 0, length)
                    }
                }
            }

            values.apply {
                put(MediaStore.MediaColumns.TITLE, resourceName)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                put(MediaStore.MediaColumns.SIZE, ringtoneFile.length())
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val newUri: Uri?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, ringtoneFile.name)
                newUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

                newUri?.let {
                    contentResolver.openOutputStream(it).use { os ->
                        val bytes = ByteArray(ringtoneFile.length().toInt())
                        BufferedInputStream(FileInputStream(ringtoneFile)).use { buf ->
                            buf.read(bytes)
                            os?.write(bytes)
                        }
                    }
                    RingtoneManager.setActualDefaultRingtoneUri(this, ringtoneType, it)
                }
            } else {
                values.put(MediaStore.MediaColumns.DATA, ringtoneFile.absolutePath)
                val uri = MediaStore.Audio.Media.getContentUriForPath(ringtoneFile.absolutePath)

                contentResolver.delete(uri!!, "${MediaStore.MediaColumns.DATA}=\"${ringtoneFile.absolutePath}\"", null)
                newUri = contentResolver.insert(uri, values)

                RingtoneManager.setActualDefaultRingtoneUri(this, ringtoneType, newUri)
                contentResolver.insert(MediaStore.Audio.Media.getContentUriForPath(ringtoneFile.absolutePath)!!, values)
            }

            Toast.makeText(this, toastMSG, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
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

            stopPlayback()

            playRingtone(ringtone.resourceId)
        }
    }

    private fun playCurrentRingtone() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            ringtoneUri = Uri.parse("android.resource://${applicationContext.packageName}/${ringtone.resourceId}")
            binding.ringtoneTitleTextView.text = ringtone.title
            binding.ringtoneAuthorTextView.text = ringtone.author
            binding.fullDurationTime.text = "Loading..."
            binding.completionLine.progress = 0

            playRingtone(ringtone.resourceId)
        }
    }

    private fun playRingtone(resourceId: String) {
        try {
            val uri = Uri.parse("android.resource://${applicationContext.packageName}/$resourceId")

            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                    reset()
                }
                setDataSource(applicationContext, uri)
                setOnPreparedListener { mp ->
                    mp.start()
                    binding.playPauseRingtone.setImageResource(R.drawable.baseline_pause_24)
                    binding.completionLine.max = mp.duration
                    binding.fullDurationTime.text = formatDuration(mp.duration)
                    updateHandler.post(updateRunnable)
                }
                prepareAsync()
            } ?: run {
                mediaPlayer = MediaPlayer().apply {
                    setOnCompletionListener {
                        onRingtonePlayCompleted()
                    }
                    setDataSource(applicationContext, uri)
                    setOnPreparedListener { mp ->
                        mp.start()
                        binding.playPauseRingtone.setImageResource(R.drawable.baseline_pause_24)
                        binding.completionLine.max = mp.duration
                        binding.fullDurationTime.text = formatDuration(mp.duration)
                        updateHandler.post(updateRunnable)
                    }
                    prepareAsync()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error playing ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
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

    companion object {
        private const val REQUEST_CODE_WRITE_SETTINGS = 200
        const val EXTRA_RINGTONE_LIST = "extra_ringtone_list"
        const val EXTRA_PLAYING_POSITION = "extra_playing_position"
    }
}