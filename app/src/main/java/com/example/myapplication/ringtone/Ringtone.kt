package com.example.myapplication.ringtone

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentRingtoneBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class Ringtone : Fragment(), GestureDetector.OnGestureListener {
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
    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f

    companion object {
        const val MINI_DISTANCE = 150
    }

    private var currentFilter: Int? = 1

    private val handler = Handler(Looper.getMainLooper())
    private val debounceDelay: Long = 300
    private lateinit var binding: FragmentRingtoneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RingtoneViewModel::class.java)
    }

    @SuppressLint("ClickableViewAccessibility")
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
        gestureDetector = GestureDetector(requireContext(), this)
        navController = findNavController()

        viewModel.ringtones.observe(viewLifecycleOwner) { ringtones ->
            ringtoneList.clear()
            ringtoneList.addAll(ringtones)
            adapter.notifyDataSetChanged()
            hideSpinner()
            updateNotFoundMessage(ringtoneList.isEmpty())
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

        if (viewModel.hasLoadedRingtones()) {
            // If ringtones are already loaded, just apply the current filter
            filterRingtones(currentFilter)
        } else {
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

        swipeRefreshLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            true
        }

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            true
        }

        setupCategoryFilters()
        return view
    }

    private fun setupCategoryFilters() {
        binding.allFilter.setOnClickListener { applyFilter(1) }
        binding.ringtoneFilter.setOnClickListener { applyFilter(2) }
        binding.notificationFilter.setOnClickListener { applyFilter(3) }
        binding.alarmFilter.setOnClickListener { applyFilter(4) }
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        if (x1 == 0f && y1 == 0f) return // Check initial values

        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) > MINI_DISTANCE && abs(deltaY) < MINI_DISTANCE) {
            if (deltaX > 0) {
                applyGeneralFilter("swipe_left")
            } else {
                applyGeneralFilter("swipe_right")
            }
        }

        // Reset touch start position
        x1 = 0f
        y1 = 0f
    }

    private fun applyFilter(filter: Int) {
        clearFilterBackgrounds()
        when (filter) {
            1 -> {
                binding.allFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                binding.allFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                currentFilter = 1
                filterRingtones(1)
            }
            2 -> {
                binding.ringtoneFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                binding.ringtoneFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                currentFilter = 2
                filterRingtones(2)
            }
            3 -> {
                binding.notificationFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                binding.notificationFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                currentFilter = 3
                filterRingtones(3)
            }
            4 -> {
                binding.alarmFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                binding.alarmFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                currentFilter = 4
                filterRingtones(4)
            }
        }
    }

    private fun applyGeneralFilter(action: String) {
        when (action) {
            "swipe_right" -> {
                if (currentFilter == 4) {
                    // NavigationHandler.navigateToDestination(navController, R.id.wallpaper)
                } else {
                    val newFilter = (currentFilter ?: 1) + 1
                    applyFilter(newFilter.coerceAtMost(4)) // Ensure the filter value does not exceed 4
                }
            }
            "swipe_left" -> {
                if (currentFilter == 1) {
                    // NavigationHandler.navigateToDestination(navController, R.id.home)
                } else {
                    val newFilter = (currentFilter ?: 1) - 1
                    applyFilter(newFilter.coerceAtLeast(1)) // Ensure the filter value does not go below 1
                }
            }
        }
    }

    private fun clearFilterBackgrounds() {
        binding.ringtoneFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.defaultBackgroundColor))
        binding.notificationFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.defaultBackgroundColor))
        binding.alarmFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.defaultBackgroundColor))
        binding.allFilter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.defaultBackgroundColor))
        binding.ringtoneFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.notificationFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.alarmFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.allFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun filterRingtones(filter: Int?) {
        val categoryFilter = when (filter) {
            1 -> null
            2 -> "ringtone"
            3 -> "notification"
            4 -> "alarm"
            else -> null
        }

        viewModel.ringtones.value?.let { ringtones ->
            val filteredList = if (categoryFilter.isNullOrEmpty()) {
                ringtones // No filter applied, show all ringtones
            } else {
                ringtones.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            }
            ringtoneList.clear()
            ringtoneList.addAll(filteredList)
            adapter.notifyDataSetChanged()
            updateNotFoundMessage(filteredList.isEmpty())
        }
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
                    val icon = document.getString("imageUrl") ?: "https://firebasestorage.googleapis.com/v0/b/andriodthemeapp.appspot.com/o/Ringtone_Database%2Ficons%2Fdefault_iconn.png?alt=media&token=c3f39156-382d-48c3-a3b8-a81c23ba4ef9"
                    val author = document.getString("artist") ?: "Unknown"
                    val category = document.getString("category") ?: "Unknown"
                    if (resourceId.isNotEmpty()) {
                        val duration = getRingtoneDuration(resourceId)
                        ringtones.add(RingtoneItem(title, resourceId, duration, author, category, icon))
                    }
                }
                withContext(Dispatchers.Main) {
                    viewModel.setRingtones(ringtones)
                    filterRingtones(currentFilter)
                    hideSpinner()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    notFoundTextView.visibility = View.VISIBLE
                    notFoundTextView.text = "Error: ${e.message}"
                    hideSpinner()
                }
            }
        }
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

    private fun updateNotFoundMessage(isEmpty: Boolean = false) {
        notFoundTextView.visibility = if (isEmpty && searchView.query.isEmpty()) View.VISIBLE else View.GONE
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

    private fun clearSearchField() {
        searchView.setQuery("", false) // Clear the query and do not submit it
    }

    private fun collapseSearchView() {
        clearSearchField()
        filterRingtones(currentFilter)
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

    override fun onDown(e: MotionEvent): Boolean {
        x1 = e.x
        y1 = e.y
        return false
    }

    override fun onShowPress(p0: MotionEvent) {}

    override fun onSingleTapUp(p0: MotionEvent): Boolean = false

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean = false

    override fun onLongPress(p0: MotionEvent) {}

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean = false
}
