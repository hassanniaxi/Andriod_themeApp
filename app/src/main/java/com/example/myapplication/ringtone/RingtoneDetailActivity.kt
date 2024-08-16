package com.example.myapplication.ringtone

import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityRingtoneDetailBinding

class RingtoneDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRingtoneDetailBinding
    private val items = arrayOf("Phone ringtone", "Alarm tone", "Notification tone")
    private var checkedItem = -1
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var ringtoneList: List<RingtoneItem>

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

        intent?.let {
            val ringtoneTitle = it.getStringExtra(EXTRA_RINGTONE_TITLE)
            val ringtoneAuthor = it.getStringExtra(EXTRA_RINGTONE_AUTHOR)
            currentlyPlayingPosition = it.getIntExtra(EXTRA_PLAYING_POSITION, RecyclerView.NO_POSITION)
            ringtoneList = it.getParcelableArrayListExtra(EXTRA_RINGTONE_LIST) ?: listOf()

            if (currentlyPlayingPosition in ringtoneList.indices) {
                binding.fullDurationTime.text = formatDuration(ringtoneList[currentlyPlayingPosition].duration)
                binding.ringtoneTitleTextView.text = ringtoneTitle
                binding.ringtoneAuthorTextView.text = ringtoneAuthor
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
            showRingtoneDialog()
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
                setDataSource(applicationContext, Uri.parse(resourceId))
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

    private fun showRingtoneDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Ringtone On")
        builder.setSingleChoiceItems(items, checkedItem) { _, which ->
            checkedItem = which
        }

        builder.setPositiveButton("Done") { _, _ ->
            checkWriteSettingsPermission()
        }

        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _ ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun applyRingtone() {
        val ringtone = ringtoneList.getOrNull(currentlyPlayingPosition) ?: return
        val ringtoneUri = "android.resource://${packageName}/${ringtone.resourceId}"

        val ringtoneType = when (checkedItem) {
            0 -> RingtoneManager.TYPE_RINGTONE
            1 -> RingtoneManager.TYPE_ALARM
            2 -> RingtoneManager.TYPE_NOTIFICATION
            else -> return
        }

        try {
            val uri = Uri.parse(ringtoneUri)
            RingtoneManager.setActualDefaultRingtoneUri(this, ringtoneType, uri)
            Toast.makeText(this, "Ringtone applied successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying ringtone: ${e.message}")
            Toast.makeText(this, "Failed to apply ringtone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            applyRingtone()
        }
    }

    private fun updateUI() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            binding.ringtoneTitleTextView.text = ringtone.title
            binding.ringtoneAuthorTextView.text = ringtone.author
        }
    }

    companion object {
        private const val TAG = "RingtoneDetailActivity"
        const val EXTRA_RINGTONE_TITLE = "extra_ringtone_title"
        const val EXTRA_RINGTONE_AUTHOR = "extra_ringtone_author"
        const val EXTRA_PLAYING_POSITION = "extra_playing_position"
        const val EXTRA_RINGTONE_LIST = "extra_ringtone_list"
    }
}
