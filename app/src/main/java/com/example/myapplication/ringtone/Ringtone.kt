package com.example.myapplication.ringtone

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.NavigationHandler
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentRingtoneBinding
import com.example.walltone.ringtone.RingtoneAdapter
import kotlinx.coroutines.*
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
    private lateinit var viewModel: RingtoneViewModel
    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector
    private lateinit var sortTextView: LinearLayout
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f

    companion object {
        const val MINI_DISTANCE = 50
    }

    private var currentFilter: Int? = 1

    private val handler = Handler(Looper.getMainLooper())
    private val debounceDelay: Long = 300
    private lateinit var binding: FragmentRingtoneBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRingtoneBinding.inflate(inflater, container, false)
        val view = binding.root

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
        adapter = RingtoneAdapter(ringtoneList, requireContext(), findNavController())
        recyclerView.adapter = adapter

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
            loadRingtonesByDrawable()
        }else{
                filterRingtones(currentFilter)
            }
        setFilterButtonState(binding.allFilter, binding.alarmFilter,binding.ringtoneFilter,binding.notificationFilter)

        ringtoneSearchButton.setOnClickListener {
            toggleSearchView()
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadRingtonesByDrawable()
        }

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
          false
        }
        toTransfer()
        setupCategoryFilters()
        return view
    }
    private  fun toTransfer(){
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.ringtones) {
                        requireActivity().finish()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        )
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
                navController.let { NavigationHandler.navigateToDestination(it, R.id.icon_changer) }
            }
        }

        x1 = 0f
        y1 = 0f
    }

    private fun applyFilter(filter: Int) {
        when (filter) {
            1 -> {
                setFilterButtonState(binding.allFilter, binding.alarmFilter,binding.ringtoneFilter,binding.notificationFilter)
                currentFilter = 1
                filterRingtones(1)
            }
            2 -> {
                setFilterButtonState(binding.ringtoneFilter, binding.alarmFilter,binding.allFilter,binding.notificationFilter)
                currentFilter = 2
                filterRingtones(2)
            }
            3 -> {
                setFilterButtonState(binding.notificationFilter, binding.alarmFilter,binding.ringtoneFilter,binding.allFilter)
                currentFilter = 3
                filterRingtones(3)
            }
            4 -> {
                setFilterButtonState(binding.alarmFilter,binding.allFilter, binding.ringtoneFilter,binding.notificationFilter)
                currentFilter = 4
                filterRingtones(4)
            }
        }
    }

    private fun setFilterButtonState(selectedButton: TextView, otherButton1: TextView,otherButton2: TextView,otherButton3: TextView) {
        val selectedDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_filter_button_selected)
        val defaultDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_filter_button_default)
        selectedButton.background = selectedDrawable
        otherButton1.background = defaultDrawable
        otherButton2.background = defaultDrawable
        otherButton3.background = defaultDrawable
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        otherButton1.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        otherButton2.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        otherButton3.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
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

            when (binding.sortBy.text.toString()) {
                "Name" -> sortRingtones()
                "Artist" -> sortByArtist()
            }

            adapter.notifyDataSetChanged()
            updateNotFoundMessage(filteredList.isEmpty())
        }
    }

    private fun loadRingtonesByDrawable() {
        context?.let { nonNullContext ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val ringtoneList = listOf(
                        RingtoneItem("Iphone X", "iphone_x", "Apple", "Ringtone"),
                        RingtoneItem("Classic Tone", "classic_tone", "ALIKHAN", "Ringtone"),
                        RingtoneItem("Parasyte-Black Nail", "parasyte_black_nail_v2", "BAKANEE", "Ringtone"),
                        RingtoneItem("Despacito", "despacito", "ZEDGE", "Ringtone"),
                        RingtoneItem("Mafia", "mafia_ringtone", "Zilu", "Ringtone"),
                        RingtoneItem("Rockstar", "rockstar", "Diablo", "Ringtone"),
                        RingtoneItem("Kids - Stranger things", "kids_stranger_things", "Lucikitten", "Ringtone"),
                        RingtoneItem("Old Phone Remix", "old_phone_remix", "Nathan", "Ringtone"),
                        RingtoneItem("Nucleya", "nucleya_ringtone", "Ankitraaj", "Ringtone"),
                        RingtoneItem("Zeus", "zeus_ringtone", "Gozmo2o", "Ringtone"),
                        RingtoneItem("Office", "office_ring", "Unknown", "Ringtone"),
                        RingtoneItem("Iphone Alarm", "iphone_alarm", "Apple", "Alarm"),
                        RingtoneItem("Bling Bling", "iphone_message", "Apple", "Alarm"),
                        RingtoneItem("Morning", "morning_alarm", "Unknown", "Alarm"),
                        RingtoneItem("Tik Tok", "tik_tok_alarm", "Unknown", "Alarm"),
                        RingtoneItem("Loud", "loud_alarm_sound", "Hardik", "Alarm"),
                        RingtoneItem("Classic", "alarm_classic", "Dragos", "Alarm"),
                        RingtoneItem("Emergency", "emergency", "Rcrumbley", "Alarm"),
                        RingtoneItem("Extreme Clock", "extreme_alarm_clock", "ERlCA", "Alarm"),
                        RingtoneItem("Fancy", "fansy", "Unknown", "Alarm"),
                        RingtoneItem("Classic Birds", "classic_birds", "DarkObsessions", "Alarm"),
                        RingtoneItem("Aurora", "aurora", "Smc Dev", "Notification"),
                        RingtoneItem("Pookebool", "pookebool", "BShacklebolt", "Notification"),
                        RingtoneItem("Pluck", "pluck", "Unknown", "Notification"),
                        RingtoneItem("Ch ch", "ch_ch", "Susanaherr", "Notification"),
                        RingtoneItem("Tono", "tono", "mxmxo", "Notification"),
                        RingtoneItem("Samsung Pay", "samsung_pay", "Unknown", "Notification"),
                        RingtoneItem("Oyoy", "oyoy", "Jatsuuu", "Notification"),
                        RingtoneItem("2008", "ok_2008", "Unknown", "Notification"),
                        RingtoneItem("Poing poing", "poing_poing", "Okade", "Notification"),
                        RingtoneItem("Nokia 2007", "nokia_2007", "Unknown", "Notification")
                    )

                    val ringtones = ringtoneList.mapNotNull { ringtone ->
                        val resourceId = nonNullContext.resources.getIdentifier(ringtone.resourceId, "raw", nonNullContext.packageName)
                        if (resourceId != 0) {
                            RingtoneItem(ringtone.title, resourceId.toString(), ringtone.author, ringtone.category)
                        } else {
                            null
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
        } ?: run {
            notFoundTextView.visibility = View.VISIBLE
            notFoundTextView.text = "Error: Context is null."
            hideSpinner()
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
        searchView.setQuery("", false)
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