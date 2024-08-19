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
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentRingtoneBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

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
    private lateinit var binding: FragmentRingtoneBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRingtoneBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize views using binding
        recyclerView = binding.ringtoneRecyclerView
        swipeRefreshLayout = binding.swipeRefreshLayoutRingtone
        notFoundTextView = binding.notFoundTextView
        spinner = binding.spinner
        searchView = binding.ringtoneSearchView
        ringtoneSearchButton = binding.ringtoneSearchButton

        viewModel = ViewModelProvider(this).get(RingtoneViewModel::class.java)

        viewModel.ringtones.observe(viewLifecycleOwner) { ringtones ->
            ringtoneList.clear()
            ringtoneList.addAll(ringtones)
            adapter.notifyDataSetChanged()
            hideSpinner()
            notFoundTextView.visibility = if (ringtoneList.isEmpty()) View.VISIBLE else View.GONE
        }

        // Change text color to white
        searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.WHITE)
        }

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
        CoroutineScope(Dispatchers.IO).launch {
            db = FirebaseFirestore.getInstance()
            try {
                val result = db.collection("ringtones").get().await()
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
                withContext(Dispatchers.Main) {
                    viewModel.setRingtones(ringtones)
                    hideSpinner()
                    if (ringtones.isEmpty()) {
                        notFoundTextView.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideSpinner()
                    notFoundTextView.visibility = View.VISIBLE
                    notFoundTextView.text = "Error: ${e.message}"
                }
            }
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

    private fun collapseSearchView() {
        searchView.animate().alpha(0f).setDuration(300).withEndAction {
            searchView.visibility = View.GONE
            searchView.clearFocus()
        }
        ringtoneSearchButton.setImageResource(R.drawable.baseline_search_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        // Release other resources if needed
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
