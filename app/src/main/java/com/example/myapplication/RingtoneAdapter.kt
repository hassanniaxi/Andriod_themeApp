package com.example.myapplication

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RingtoneAdapter(
    private var ringtones: List<RingtoneItem>,
    private val context: Context,
    private val mainActivity: MainActivity // Reference to MainActivity to access search view state
) : RecyclerView.Adapter<RingtoneAdapter.ViewHolder>(), Filterable {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION
    private val handler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null
    private var ringtonesFiltered: List<RingtoneItem> = ringtones // Initially unfiltered

    private var fullDuration: Int = 0 // Variable to store full duration when search view is open

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.ringtone_title)
        val playButton: ImageButton = view.findViewById(R.id.play_button)
        val completionLine: ProgressBar = view.findViewById(R.id.completion_line)
        val ringtonePlayDuration: TextView = view.findViewById(R.id.play_time)
        val ringtoneFullDuration: TextView = view.findViewById(R.id.full_duration_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ringtone_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ringtone = ringtonesFiltered[position]
        holder.titleTextView.text = ringtone.title
        holder.ringtoneFullDuration.text = formatDuration(ringtone.duration)


        val isPlaying = position == currentlyPlayingPosition
        holder.playButton.setImageResource(
            if (isPlaying) R.drawable.pauseicon else R.drawable.play
        )

        if (isPlaying) {
            holder.completionLine.max = mediaPlayer?.duration ?: 0
            //holder.ringtoneFullDuration.text = formatDuration(mediaPlayer?.duration ?: 0)
            holder.ringtonePlayDuration.text = formatDuration(mediaPlayer?.currentPosition ?: 0)
            updateProgress(holder, position)
        } else {
            holder.completionLine.progress = 0
            holder.ringtonePlayDuration.text = formatDuration(0)
         //   holder.ringtoneFullDuration.text = formatDuration(holder.completionLine.max)
        }

        holder.playButton.setOnClickListener {
            if (isPlaying) {
                stopPlayback()
            } else {
                playRingtone(holder.itemView.context, ringtone.resourceId, position)
            }
        }
    }

    private fun playRingtone(context: Context, resourceId: Int, position: Int) {
        stopPlayback()

        mediaPlayer = MediaPlayer.create(context, resourceId).apply {
            setOnCompletionListener {
                stopPlayback()
            }
            start()
        }

        currentlyPlayingPosition = position
        notifyItemChanged(position)

        getViewHolder(position)?.let {
            it.ringtoneFullDuration.text = formatDuration(mediaPlayer?.duration ?: 0)
            it.ringtonePlayDuration.text = formatDuration(mediaPlayer?.currentPosition ?: 0)
            updateProgress(it, position)
        }

        // Store full duration when search view is open
        if (mainActivity.isSearchViewExpanded) {
            fullDuration = mediaPlayer?.duration ?: 0
        }
    }

    private fun updateProgress(holder: ViewHolder, position: Int) {
        updateProgressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    val currentPosition = player.currentPosition
                    val totalDuration = player.duration

                    if (currentPosition >= totalDuration) {
                        stopPlayback()
                        return
                    }

                    Log.d("RingtoneAdapter", "Current Position: $currentPosition, Total Duration: $totalDuration")

                    holder.apply {
                        // Update ProgressBar
                        completionLine.max = totalDuration
                        completionLine.progress = currentPosition

                        // Update play duration TextView
                        ringtonePlayDuration.text = formatDuration(currentPosition)
                    }

                    // Schedule the next update
                    handler.postDelayed(this, 1000)
                }
            }
        }
        // Start the first update
        handler.post(updateProgressRunnable!!)
    }

    private fun formatDuration(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }


    private fun getViewHolder(position: Int): ViewHolder? {
        return (getRvInstance()?.findViewHolderForAdapterPosition(position) as? ViewHolder)
    }

    private fun getRvInstance(): RecyclerView? {
        // Implement a way to get your RecyclerView instance
        return null // Replace with actual implementation
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        updateProgressRunnable?.let { handler.removeCallbacks(it) }
        updateProgressRunnable = null
        currentlyPlayingPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun release() {
        stopPlayback()
        handler.removeCallbacksAndMessages(null)
    }

    override fun getItemCount() = ringtonesFiltered.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterPattern = constraint?.toString()?.lowercase()?.trim() ?: ""
                ringtonesFiltered = if (filterPattern.isEmpty()) {
                    ringtones
                } else {
                    ringtones.filter {
                        it.title.lowercase().contains(filterPattern)
                    }
                }
                return FilterResults().apply { values = ringtonesFiltered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                ringtonesFiltered = (results?.values as? List<RingtoneItem>) ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
