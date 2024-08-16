package com.example.myapplication.ringtone

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class Ringtone : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RingtoneAdapter
    private lateinit var notFoundTextView: TextView
    private lateinit var searchView: SearchView
    private lateinit var spinner: ProgressBar
    private lateinit var ringtoneSearchButton: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val ringtoneList = mutableListOf<RingtoneItem>()
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: RingtoneViewModel

    private val handler = Handler(Looper.getMainLooper())
    private val debounceDelay: Long = 300 // Debounce delay for search input

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ringtone, container, false)

        recyclerView = view.findViewById(R.id.ringtone_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout_ringtone)
        notFoundTextView = view.findViewById(R.id.not_found_text_view)
        spinner = view.findViewById(R.id.spinner)

        // Access SearchView and ImageButton from MainActivity
        (requireActivity() as MainActivity).apply {
            searchView = getSearchView()
            ringtoneSearchButton = getRingtoneSearchButton()
        }

        viewModel = ViewModelProvider(this).get(RingtoneViewModel::class.java)

        viewModel.ringtones.observe(viewLifecycleOwner) { ringtones ->
            ringtoneList.clear()
            ringtoneList.addAll(ringtones)
            adapter.notifyDataSetChanged()
            hideSpinner()
            if (ringtoneList.isEmpty()) {
                notFoundTextView.visibility = View.VISIBLE
            }
        }

        // Change text color to white
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.WHITE)
        }

        // Change the color of the search and close icons
        searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)?.setColorFilter(Color.WHITE)
        searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)?.setColorFilter(Color.WHITE)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RingtoneAdapter(ringtoneList, requireContext())
        recyclerView.adapter = adapter

        // Set up SearchView with debounce
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ performSearch(newText.orEmpty()) }, debounceDelay)
                return true
            }
        })

        if (ringtoneList.isEmpty()) {
            showSpinner()
            loadRingtones()
        }

        // Handle Search Button click
        ringtoneSearchButton.setOnClickListener {
            toggleSearchView()
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadRingtones()
        }

        return view
    }

    private fun loadRingtones() {
        db = FirebaseFirestore.getInstance()
        db.collection("ringtones").get()
            .addOnSuccessListener { result ->
                val ringtones = mutableListOf<RingtoneItem>()
                for (document in result) {
                    val title = document.getString("name") ?: "Unknown"
                    val resourceId = document.getString("musicUrl") ?: ""
                    val icon = document.getString("imageUrl") ?: ""
                    val author = document.getString("artist") ?: "Unknown"
                    if (resourceId.isNotEmpty()) {
                        ringtones.add(RingtoneItem(title, resourceId, getRingtoneDuration(resourceId), author, icon))
                    }
                }
                viewModel.setRingtones(ringtones)
                if (ringtones.isEmpty()) {
                    notFoundTextView.visibility = View.VISIBLE
                }
                hideSpinner() // Ensure spinner is hidden after success
            }
            .addOnFailureListener { exception ->
                hideSpinner()
                notFoundTextView.visibility = View.VISIBLE
                notFoundTextView.text = "Error: ${exception.message}"
            }
    }

    private fun showSpinner() {
        spinner.visibility = View.VISIBLE
        notFoundTextView.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    private fun hideSpinner() {
        spinner.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false
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

    private fun toggleSearchView() {
        if (searchView.visibility == View.VISIBLE) {
            collapseSearchView()
        } else {
            searchView.visibility = View.VISIBLE
            searchView.requestFocus()
            searchView.animate().alpha(1f).setDuration(300).setListener(null)
            ringtoneSearchButton.setImageResource(R.drawable.baseline_close_24)
        }
    }

    fun collapseSearchView() {
        searchView.animate().alpha(0f).setDuration(300).withEndAction {
            searchView.visibility = View.GONE
            searchView.clearFocus()
        }
        ringtoneSearchButton.setImageResource(R.drawable.baseline_search_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    private fun getRingtoneDuration(resourceId: String): Int {
        return try {
            val mediaPlayer = MediaPlayer().apply {
                context?.let { setDataSource(it, Uri.parse(resourceId)) }
                prepare()
            }
            val duration = mediaPlayer.duration
            mediaPlayer.release()
            duration
        } catch (e: Exception) {
            0
        }
    }
}
