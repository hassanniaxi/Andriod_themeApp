package com.example.myapplication.ringtone

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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

class RingtoneDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRingtoneDetailBinding
    private lateinit var bindingForLoading: OverlaySpinnerLayoutBinding
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION
    private lateinit var ringtoneList: List<RingtoneItem>

    private val updateHandler = Handler()
    private val updateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                binding.completionLine.progress = it.currentPosition
                binding.ringtonePlayDuration.text = formatDuration(it.currentPosition)
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
            currentlyPlayingPosition = it.getIntExtra(EXTRA_PLAYING_POSITION, RecyclerView.NO_POSITION)
            ringtoneList = it.getParcelableArrayListExtra(EXTRA_RINGTONE_LIST) ?: listOf()
            setupRingtoneData(it)
        } ?: run {
            finish()
        }

        setupListeners()
        playCurrentRingtone()
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS && Settings.System.canWrite(this)) {
            showRingtoneBottomDialog()
        } else {
            Toast.makeText(this, "Permission denied. Cannot set ringtone.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRingtoneData(intent: Intent) {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            binding.fullDurationTime.text = formatDuration(ringtone.duration)
            binding.ringtoneTitleTextView.text = intent.getStringExtra(EXTRA_RINGTONE_TITLE)
            binding.ringtoneAuthorTextView.text = intent.getStringExtra(EXTRA_RINGTONE_AUTHOR)
            Glide.with(this).load(ringtone.icon).into(binding.ringtoneIcon)
        }
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
            if (mediaPlayer?.isPlaying == true) pausePlayback() else resumePlayback()
        }

        binding.previousRingtone.setOnClickListener {
            playPreviousRingtone()
        }

        binding.nextRingtone.setOnClickListener {
            playNextRingtone()
        }
    }

    private fun playCurrentRingtone() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let {
            playRingtone(it.resourceId)
        }
    }

    private fun showRingtoneBottomDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.bottom_sheet_dialog_apply_ringtone)
            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                attributes?.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.BOTTOM)
            }
        }

        dialog.findViewById<LinearLayout>(R.id.setOnPhone).setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_RINGTONE)
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.setOnAlarm).setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_ALARM)
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayout>(R.id.setOnNotification).setOnClickListener {
            applyRingtone(RingtoneManager.TYPE_NOTIFICATION)
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyRingtone(type: Int) {
        showSpinner()
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            val resourceUri = "android.resource://${packageName}/${ringtone.resourceId}"
            val uri = Uri.parse(resourceUri)
            try {
                RingtoneManager.setActualDefaultRingtoneUri(this, type, uri)
                Toast.makeText(this, "Ringtone applied successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to apply ringtone", Toast.LENGTH_SHORT).show()
            }
            hideSpinner()
        }
    }

    private fun checkWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    private fun pausePlayback() {
        mediaPlayer?.pause()
        binding.playPauseRingtone.setImageResource(R.drawable.baseline_play_arrow_24)
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        binding.playPauseRingtone.setImageResource(R.drawable.baseline_pause_24)
        updateHandler.post(updateRunnable)
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
            e.printStackTrace()
        }
    }

    private fun playPreviousRingtone() {
        currentlyPlayingPosition = (currentlyPlayingPosition - 1).takeIf { it >= 0 } ?: ringtoneList.size - 1
        updateUIForCurrentRingtone()
    }

    private fun playNextRingtone() {
        currentlyPlayingPosition = (currentlyPlayingPosition + 1) % ringtoneList.size
        updateUIForCurrentRingtone()
    }

    private fun updateUIForCurrentRingtone() {
        ringtoneList.getOrNull(currentlyPlayingPosition)?.let { ringtone ->
            binding.fullDurationTime.text = formatDuration(ringtone.duration)
            binding.ringtoneTitleTextView.text = ringtone.title
            binding.ringtoneAuthorTextView.text = ringtone.author
            Glide.with(this).load(ringtone.icon).into(binding.ringtoneIcon)
            stopPlayback()
            playRingtone(ringtone.resourceId)
        }
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

    companion object {
        private const val REQUEST_CODE_WRITE_SETTINGS = 200
        const val EXTRA_RINGTONE_LIST = "extra_ringtone_list"
        const val EXTRA_PLAYING_POSITION = "extra_playing_position"
        const val EXTRA_RINGTONE_TITLE = "extra_ringtone_title"
        const val EXTRA_RINGTONE_AUTHOR = "extra_ringtone_author"
    }
}
