package com.example.myapplication.ringtone

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityRingtoneDetailBinding
import com.example.myapplication.databinding.OverlaySpinnerLayoutBinding
import java.io.File
import java.io.FileOutputStream

class RingtoneDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRingtoneDetailBinding
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var ringtoneList: List<RingtoneItem>
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private val updateHandler = Handler()
    private val updateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                val currentPosition = it.currentPosition
                binding.completionLine.progress = currentPosition
                binding.ringtonePlayDuration.text = formatDuration(currentPosition)
                updateHandler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRingtoneDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindingForLoading = OverlaySpinnerLayoutBinding.inflate(layoutInflater)
        binding.root.addView(bindingForLoading.root)

        intent?.let {
            val ringtoneTitle = it.getStringExtra(EXTRA_RINGTONE_TITLE)
            val ringtoneAuthor = it.getStringExtra(EXTRA_RINGTONE_AUTHOR)
            currentlyPlayingPosition = it.getIntExtra(EXTRA_PLAYING_POSITION, RecyclerView.NO_POSITION)
            ringtoneList = it.getParcelableArrayListExtra(EXTRA_RINGTONE_LIST) ?: listOf()

            if (currentlyPlayingPosition in ringtoneList.indices) {
                val ringtone = ringtoneList[currentlyPlayingPosition]
                binding.fullDurationTime.text = formatDuration(ringtone.duration)
                binding.ringtoneTitleTextView.text = ringtoneTitle
                binding.ringtoneAuthorTextView.text = ringtoneAuthor
                Glide.with(this)
                    .load(ringtone.icon)
                    .into(binding.ringtoneIcon)
            } else {
                Log.e(TAG, "Invalid playing position: $currentlyPlayingPosition")
                finish()
            }
        } ?: run {
            Log.e(TAG, "No intent data available")
            finish()
        }

        setupListeners()

        if (currentlyPlayingPosition in ringtoneList.indices) {
            playRingtone(ringtoneList[currentlyPlayingPosition].resourceId)
        }
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.let {
            updateHandler.post(updateRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        updateHandler.removeCallbacks(updateRunnable)
    }

    private fun setupListeners() {
        binding.backToRingtones.setOnClickListener {
            stopPlayback()
            finish()
        }

        binding.applyRingtoneOn.setOnClickListener {
            checkWriteSettingsPermission()
        }

        binding.playPauseRingtone.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                pausePlayback()
            } else {
                ringtoneList.getOrNull(currentlyPlayingPosition)?.let {
                    resumePlayback()
                }
            }
        }

        binding.previousRingtone.setOnClickListener {
            playPreviousRingtone()
        }

        binding.nextRingtone.setOnClickListener {
            playNextRingtone()
        }
    }

    private fun showRingtoneBottomDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_dialog_apply_ringtone)

        val onPhone = dialog.findViewById<LinearLayout>(R.id.setOnPhone)
        val onAlarm = dialog.findViewById<LinearLayout>(R.id.setOnAlarm)
        val onNotification = dialog.findViewById<LinearLayout>(R.id.setOnNotification)
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)

        onPhone.setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_RINGTONE)
            dialog.dismiss()
        }

        onNotification.setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_NOTIFICATION)
            dialog.dismiss()
        }

        onAlarm.setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_ALARM)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.attributes?.windowAnimations = R.style.DialogAnimation
            it.setGravity(Gravity.BOTTOM)
        }
    }

    private fun applyRingtone(type: Int) {
        val ringtone = ringtoneList.getOrNull(currentlyPlayingPosition) ?: return
        val ringtoneUri = "android.resource://${packageName}/${ringtone.resourceId}"
        val uri = Uri.parse(ringtoneUri)
        Log.d(TAG, "Applying ringtone with URI: $uri")

        try {
            // Create a file and save the ringtone
            val fileName = "${ringtone.title}.mp3"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)

            val outputStream = FileOutputStream(file)
            val inputStream = contentResolver.openInputStream(uri) ?: return
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()

            // Insert the ringtone into media store
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                put(MediaStore.MediaColumns.TITLE, ringtone.title)
                put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneManager.TYPE_NOTIFICATION)
                put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val resolver = contentResolver
            val newUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (newUri != null) {
                // Set the new URI as the default ringtone
                RingtoneManager.setActualDefaultRingtoneUri(this, type, newUri)
                Toast.makeText(this, "Ringtone applied successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to insert ringtone into media store.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying ringtone: ${e.message}")
            Toast.makeText(this, "Failed to apply ringtone", Toast.LENGTH_SHORT).show()
        } finally {
            hideSpinner()
        }
    }


    private fun checkWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
        } else {
            showRingtoneBottomDialog()
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

    private fun playRingtone(resourceId: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@RingtoneDetailActivity, Uri.parse(resourceId))
                prepare()
                start()
                binding.completionLine.max = duration
                updateHandler.post(updateRunnable)

                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                    binding.playPauseRingtone.setImageResource(R.drawable.baseline_play_arrow_24)
                    updateHandler.removeCallbacks(updateRunnable)
                    playNextRingtone()
                }

                binding.playPauseRingtone.setImageResource(R.drawable.baseline_pause_24)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing ringtone: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        binding.playPauseRingtone.setImageResource(R.drawable.baseline_play_arrow_24)
        updateHandler.removeCallbacks(updateRunnable)
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        binding.playPauseRingtone.setImageResource(R.drawable.baseline_pause_24)
        updateHandler.post(updateRunnable)
    }

    private fun formatDuration(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun playPreviousRingtone() {
        if (ringtoneList.isNotEmpty()) {
            currentlyPlayingPosition = (currentlyPlayingPosition - 1 + ringtoneList.size) % ringtoneList.size
            ringtoneList.getOrNull(currentlyPlayingPosition)?.let {
                binding.fullDurationTime.text = formatDuration(it.duration)
                playRingtone(it.resourceId)
                updateUI()
            }
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

    private fun playNextRingtone() {
        if (ringtoneList.isNotEmpty()) {
            currentlyPlayingPosition = (currentlyPlayingPosition + 1) % ringtoneList.size
            ringtoneList.getOrNull(currentlyPlayingPosition)?.let {
                binding.fullDurationTime.text = formatDuration(it.duration)
                playRingtone(it.resourceId)
                updateUI()
            }
        }
    }

    private fun updateUI() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            binding.ringtoneTitleTextView.text = ringtone.title
            binding.ringtoneAuthorTextView.text = ringtone.author
            Glide.with(this)
                .load(ringtone.icon)
                .into(binding.ringtoneIcon)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                showRingtoneBottomDialog()
            } else {
                Toast.makeText(this, "Permission denied. Cannot set ringtone.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        resumePlayback()
    }

    override fun onPause() {
        super.onPause()
        pausePlayback()
    }

    companion object {
        private const val TAG = "RingtoneDetailActivity"
        const val EXTRA_RINGTONE_TITLE = "extra_ringtone_title"
        const val EXTRA_RINGTONE_AUTHOR = "extra_ringtone_author"
        const val EXTRA_PLAYING_POSITION = "extra_playing_position"
        const val EXTRA_RINGTONE_LIST = "extra_ringtone_list"
        private const val REQUEST_CODE_WRITE_SETTINGS = 1234
    }
}
