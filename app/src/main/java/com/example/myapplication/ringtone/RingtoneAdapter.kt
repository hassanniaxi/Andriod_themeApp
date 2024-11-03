package com.example.walltone.ringtone

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.NavigationHandler
import com.example.myapplication.R
import com.example.myapplication.ringtone.RingtoneDetailActivity
import com.example.myapplication.ringtone.RingtoneItem
import kotlin.math.abs

class RingtoneAdapter(
    private var ringtones: List<RingtoneItem>,
    private val context: Context,
    private val navController: NavController
) : RecyclerView.Adapter<RingtoneAdapter.MyHolder>(), Filterable {

    private var ringtonesFiltered: List<RingtoneItem> = ringtones

    private var x1: Float = 0.0f
    private var y1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y2: Float = 0.0f

    companion object {
        private const val MINI_DISTANCE = 50
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ringtone_item, parent, false)
        return MyHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val ringtone = ringtonesFiltered[position]
        holder.titleTextView.text = ringtone.title
        holder.authorTextView.text = ringtone.author

        holder.playRingtone.setOnClickListener {
            navigateToDetailActivity(position)
        }

        holder.itemView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            false
        }
    }
    private fun navigateToDetailActivity(position: Int) {
        val intent = Intent(context, RingtoneDetailActivity::class.java).apply {
            putExtra(RingtoneDetailActivity.EXTRA_PLAYING_POSITION, position)
            putParcelableArrayListExtra(
                RingtoneDetailActivity.EXTRA_RINGTONE_LIST,
                ArrayList(ringtonesFiltered)
            )
        }
        context.startActivity(intent)
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

    inner class MyHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.ringtone_title)
        val authorTextView: TextView = view.findViewById(R.id.ringtone_author)
        val playRingtone: View = view.findViewById(R.id.play_ring)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            x1 = e.x
            y1 = e.y
            return true
        }
    })

    private fun handleSwipeGesture(event: MotionEvent) {
        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) >MINI_DISTANCE && abs(deltaY) < MINI_DISTANCE) {
            if (deltaX > 0) {
                navController.let { NavigationHandler.navigateToDestination(it, R.id.icon_changer) }
            }else{
                navController.let { NavigationHandler.navigateToDestination(it, R.id.preview) }
            }
        }
    }
}