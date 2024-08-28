package com.example.myapplication.ringtone

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
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
    private lateinit var sortTextView: LinearLayout
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
        sortTextView  = binding.sort

        viewModel = ViewModelProvider(this).get(RingtoneViewModel::class.java)

        viewModel.ringtones.observe(viewLifecycleOwner) { ringtones ->
            ringtoneList.clear()
            ringtoneList.addAll(ringtones)
            adapter.notifyDataSetChanged()
            hideSpinner()
            updateNotFoundMessage(ringtoneList.isEmpty())
        }



        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText?.apply {
            context.theme.obtainStyledAttributes(R.style.actions, intArrayOf(android.R.attr.textColor, android.R.attr.textColorHint)).apply {
                try {
                    setTextColor(getColor(0, Color.WHITE))
                    setHintTextColor(getColor(0, Color.WHITE))
                } finally {
                    recycle()
                }
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RingtoneAdapter(ringtoneList, requireContext())
        recyclerView.adapter = adapter

        // Set up SearchView with debounce
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)?.visibility = View.GONE
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ performSearch(newText.orEmpty()) }, debounceDelay)
                return true
            }
        })

        val defaultSortOption = "Name"
        binding.sortBy.setText(defaultSortOption)

        sortTextView.setOnClickListener {
            val popupMenu = PopupMenu(context, sortTextView)
            popupMenu.menuInflater.inflate(R.menu.sort_menu, popupMenu.menu)
            val sortByValue = binding.sortBy.text.toString()

            val defaultItem = popupMenu.menu.findItemByTitle(sortByValue)
            defaultItem?.let {
                highlightMenuItem(it)
            }

            popupMenu.setOnMenuItemClickListener { item ->
                val selectedOption = item.title.toString()
                binding.sortBy.text = selectedOption

                when (selectedOption) {
                    "Name" -> sortRingtones()
                    "Artist" -> {
                        sortByArtist()}
                }
                highlightMenuItem(item)
                true
            }
            popupMenu.show()
        }

            if (ringtoneList.isEmpty()) {
            showSpinner()
            loadRingtones()
        }else{
                filterRingtones(currentFilter)
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
    fun Menu.findItemByTitle(title: String): MenuItem? {
        for (i in 0 until size()) {
            val item = getItem(i)
            if (item.title.toString() == title) {
                return item
            }
        }
        return null
    }
    @SuppressLint("ResourceAsColor")
    private fun highlightMenuItem( item: MenuItem) {
        val color = context?.let { ContextCompat.getColor(it, R.color.blue) }
        val spanString = SpannableString(item.title)
        spanString.setSpan(color?.let { ForegroundColorSpan(it) }, 0, spanString.length, 0)
        item.title = spanString
    }

    private fun sortByArtist(ascending: Boolean = true) {
        ringtoneList.sortWith { item1, item2 ->
            val comparison = item1.author.compareTo(item2.author, ignoreCase = true)
            if (ascending) comparison else -comparison
        }
        adapter.notifyDataSetChanged()
    }


    private fun sortRingtones(ascending: Boolean = true) {
        ringtoneList.sortWith { item1, item2 ->
            val comparison = item1.title.compareTo(item2.title, ignoreCase = true)
            if (ascending) comparison else -comparison
        }
        adapter.notifyDataSetChanged()
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
                ringtones
            } else {
                ringtones.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            }
            ringtoneList.clear()
            ringtoneList.addAll(filteredList)

            // Apply the current sort option after filtering
            when (binding.sortBy.text.toString()) {
                "Name" -> sortRingtones()
                "Artist" -> sortByArtist()
            }

            adapter.notifyDataSetChanged()
            updateNotFoundMessage(filteredList.isEmpty())
        }
    }

    private fun loadRingtones() {
        CoroutineScope(Dispatchers.IO).launch {
            db = FirebaseFirestore.getInstance()
            try {
                val result = db.collection("ringtones")
                    .limit(20) // Use pagination, load 20 ringtones at a time
                    .get().await()

                val ringtones = result.documents.mapNotNull { document ->
                    val title = document.getString("name") ?: return@mapNotNull null
                    val resourceId = document.getString("musicUrl") ?: return@mapNotNull null
                    val author = document.getString("artist") ?: "Unknown"
                    val category = document.getString("category") ?: "Unknown"
                    RingtoneItem(title, resourceId, author, category)
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