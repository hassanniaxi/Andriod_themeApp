package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RingtoneAdapter(
    private var ringtones: List<RingtoneItem>,
    private val context: Context
) : RecyclerView.Adapter<RingtoneAdapter.ViewHolder>(), Filterable {

    private var ringtonesFiltered: List<RingtoneItem> = ringtones // Initially unfiltered
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.ringtone_title)
        val authorTextView: TextView = view.findViewById(R.id.ringtone_author)
        val playRingtone: View = view.findViewById(R.id.play_ring)
        val ringtoneIcon: ImageView = view.findViewById(R.id.ringtone_icon)

        fun bind(ringtone: RingtoneItem) {
            ringtoneIcon.setImageResource(ringtone.icon)
            titleTextView.text = ringtone.title
            authorTextView.text = "Author: ${ringtone.author}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ringtone_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ringtone = ringtonesFiltered[position]
        holder.bind(ringtone)

        holder.playRingtone.setOnClickListener {
            currentlyPlayingPosition = position
            openRingtoneDetailActivity(ringtone)
        }
    }

    private fun openRingtoneDetailActivity(ringtone: RingtoneItem) {
        val intent = Intent(context, RingtoneDetailActivity::class.java).apply {
            putExtra(RingtoneDetailActivity.EXTRA_RINGTONE_TITLE, ringtone.title)
            putExtra(RingtoneDetailActivity.EXTRA_RINGTONE_AUTHOR, ringtone.author)
            putExtra(RingtoneDetailActivity.EXTRA_PLAYING_POSITION, currentlyPlayingPosition)
            putParcelableArrayListExtra(RingtoneDetailActivity.EXTRA_RINGTONE_LIST, ArrayList(ringtonesFiltered)) // Pass ringtone list
        }
        context.startActivity(intent)
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
