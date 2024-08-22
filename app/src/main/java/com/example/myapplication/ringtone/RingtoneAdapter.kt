package com.example.myapplication.ringtone

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
import com.bumptech.glide.Glide
import com.example.myapplication.R

class RingtoneAdapter(
    private var ringtones: List<RingtoneItem>,
    private val context: Context
) : RecyclerView.Adapter<RingtoneAdapter.ViewHolder>(), Filterable {

    private var ringtonesFiltered: List<RingtoneItem> = ringtones
    private var currentlyPlayingPosition: Int = RecyclerView.NO_POSITION

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.ringtone_title)
        val authorTextView: TextView = view.findViewById(R.id.ringtone_author)
        val playRingtone: View = view.findViewById(R.id.play_ring)
        val ringtoneIcon: ImageView = view.findViewById(R.id.ringtone_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ringtone_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ringtone = ringtonesFiltered[position]
        holder.titleTextView.text = ringtone.title
        holder.authorTextView.text = ringtone.author
        Glide.with(context)
            .load(ringtone.icon)
            .into(holder.ringtoneIcon)

        holder.playRingtone.setOnClickListener {
            if (currentlyPlayingPosition != position) {

                val intent = Intent(context, RingtoneDetailActivity::class.java).apply {
                    putExtra(RingtoneDetailActivity.EXTRA_RINGTONE_TITLE, ringtone.title)
                    putExtra(RingtoneDetailActivity.EXTRA_RINGTONE_AUTHOR, ringtone.author)
                    putExtra(RingtoneDetailActivity.EXTRA_PLAYING_POSITION, position)
                    putParcelableArrayListExtra(
                        RingtoneDetailActivity.EXTRA_RINGTONE_LIST,
                        ArrayList(ringtonesFiltered) // Consider passing only minimal data
                    )
                }
                context.startActivity(intent)
            }
        }
    }


    override fun getItemCount(): Int {
        return ringtonesFiltered.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                val filteredList = if (query.isEmpty()) {
                    ringtones
                } else {
                    ringtones.filter {
                        it.title.lowercase().contains(query) ||
                                it.author.lowercase().contains(query)
                    }
                }

                return FilterResults().apply {
                    values = filteredList
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                ringtonesFiltered = results?.values as? List<RingtoneItem> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}