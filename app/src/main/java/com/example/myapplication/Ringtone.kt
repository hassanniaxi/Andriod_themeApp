package com.example.myapplication

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Ringtone : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RingtoneAdapter
    private val ringtoneList = mutableListOf<RingtoneItem>()
    private lateinit var notFoundTextView: TextView
    private lateinit var searchView: SearchView
    private lateinit var spinner: ProgressBar
    private lateinit var ringtoneSearchButton: ImageButton
    private val handler = Handler(Looper.getMainLooper())
    private val debounceRunnable = Runnable {
        performSearch(searchView.query.toString())
    }
    private val debounceDelay: Long = 300 // Debounce delay for search input

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ringtone, container, false)

        recyclerView = view.findViewById(R.id.ringtone_recycler_view)
        notFoundTextView = view.findViewById(R.id.not_found_text_view)
        spinner = view.findViewById(R.id.spinner)

        // Access SearchView and ImageButton from MainActivity
        (requireActivity() as MainActivity).apply {
            searchView = getSearchView()
            ringtoneSearchButton = getRingtoneSearchButton()
        }

        // Change text color to white
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.setTextColor(Color.WHITE)
        searchEditText?.setHintTextColor(Color.WHITE)

        // Change the color of the search icon (optional)
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon?.setColorFilter(Color.WHITE)

        // Change the color of the close icon (optional)
        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon?.setColorFilter(Color.WHITE)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RingtoneAdapter(ringtoneList, requireContext())
        recyclerView.adapter = adapter

        // Set up SearchView with debounce
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                handler.removeCallbacks(debounceRunnable)
                handler.postDelayed(debounceRunnable, debounceDelay)
                return true
            }
        })

        // Handle Search Button click
        ringtoneSearchButton.setOnClickListener {
            toggleSearchView()
        }

        showSpinner()

        return view
    }

    private fun showSpinner() {
        spinner.visibility = View.VISIBLE
        notFoundTextView.visibility = View.GONE
        recyclerView.visibility = View.GONE
        handler.postDelayed({
            loadInitialRingtones()
        }, 1000) // Simulate network delay or data loading time
    }

    private fun loadInitialRingtones() {
        if (isAdded) {
            ringtoneList.add(RingtoneItem("Mafia Ringtone", R.raw.mafia_ringtone, getRingtoneDuration(R.raw.mafia_ringtone), "Unknown", R.drawable.mafia_icon))
            ringtoneList.add(RingtoneItem("Iphone 14 pro", R.raw.ringtone_iphone_14_pro, getRingtoneDuration(R.raw.ringtone_iphone_14_pro), "Apple", R.drawable.apple_icon))

            adapter.notifyDataSetChanged()
            updateNotFoundMessage()
            spinner.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun getRingtoneDuration(resourceId: Int): Int {
        return if (isAdded) {
            val mediaPlayer = MediaPlayer.create(requireContext(), resourceId)
            val duration = mediaPlayer.duration
            mediaPlayer.release()
            duration
        } else {
            0
        }
    }

    private fun performSearch(query: String) {
        adapter.filter.filter(query)
        updateNotFoundMessage()
    }

    private fun updateNotFoundMessage() {
        notFoundTextView.visibility = if (adapter.itemCount == 0 && searchView.query.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun toggleSearchView() {
        val isVisible = searchView.visibility == View.VISIBLE

        if (isVisible) {
            collapseSearchView()
        } else {
            // Show SearchView
            searchView.visibility = View.VISIBLE
            searchView.requestFocus()
            searchView.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null)

            ringtoneSearchButton.setImageResource(R.drawable.baseline_close_24) // Change button icon to close
            ringtoneSearchButton.setOnClickListener {
                collapseSearchView()
            }
        }
    }

    fun collapseSearchView() {
        searchView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                searchView.visibility = View.GONE
                searchView.clearFocus()
            }

        ringtoneSearchButton.setImageResource(R.drawable.baseline_search_24) // Change button icon to search
        ringtoneSearchButton.setOnClickListener {
            toggleSearchView()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(debounceRunnable)
    }
}