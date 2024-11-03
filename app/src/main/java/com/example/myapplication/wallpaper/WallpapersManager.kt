package com.example.myapplication.wallpaper

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.NavigationHandler
import com.example.myapplication.databinding.FragmentWallpaperBinding
import com.example.myapplication.wallpaper.all.AllWallpaperAdapter
import com.example.myapplication.wallpaper.all.AllWallpaperViewModel
import com.example.myapplication.wallpaper.category.CatWallpaperAdapter
import com.example.myapplication.wallpaper.category.CatWallpaperItem
import com.example.myapplication.wallpaper.category.CatWallpaperViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class WallpapersManager : Fragment(), GestureDetector.OnGestureListener{

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllWallpaperAdapter
    private lateinit var adapterCat: CatWallpaperAdapter
    private lateinit var viewModel: AllWallpaperViewModel
    private lateinit var viewCatModel: CatWallpaperViewModel
    private val wallpaperList = mutableListOf<WallpaperDetailItems>()
    private val wallpaperCatList = mutableListOf<CatWallpaperItem>()
    private lateinit var notFoundTextView: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var recyclerViewState: Parcelable? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var binding: FragmentWallpaperBinding
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f
    private var navController: NavController? = null

    companion object {
        const val MINI_DISTANCE = 50
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWallpaperBinding.inflate(inflater, container, false)
        val view = binding.root
        recyclerView = view.findViewById(R.id.wallpaper_recycler_view)
        spinner = view.findViewById(R.id.spinner)
        notFoundTextView = view.findViewById(R.id.not_found_text_view2)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        navController = findNavController()
        gestureDetector = GestureDetector(requireContext(), this)

        adapter = AllWallpaperAdapter(requireContext(), wallpaperList,findNavController())
        adapterCat = CatWallpaperAdapter(requireContext(), wallpaperCatList, findNavController())

        viewModel = ViewModelProvider(this).get(AllWallpaperViewModel::class.java)
        viewCatModel = ViewModelProvider(this).get(CatWallpaperViewModel::class.java)

        initRecyclerViewForCat()
        if (wallpaperCatList.isEmpty()) {
            showSpinner()
            loadCatWallpapers()
        }
        setFilterButtonState(binding.wallpaperCategory, binding.allWallpapers)

        binding.allWallpapers.setOnClickListener {
            setFilterButtonState(binding.allWallpapers, binding.wallpaperCategory)
            initRecyclerView()
            if (wallpaperList.isEmpty()) {
                showSpinner()
                loadAllWallpapers()
            }
        }

        binding.wallpaperCategory.setOnClickListener {
            setFilterButtonState(binding.wallpaperCategory, binding.allWallpapers)
            initRecyclerViewForCat()
            if (wallpaperCatList.isEmpty()) {
                showSpinner()
                loadCatWallpapers()
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadAllWallpapers()
        }

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            false
        }

        toTransfer()
        return view
    }
    private  fun toTransfer(){
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.wallpapers) {
                        requireActivity().finish()

                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        )
    }

    private fun setFilterButtonState(selectedButton: TextView, otherButton: TextView) {
        val selectedDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_filter_button_selected)
        val defaultDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_filter_button_default)
        selectedButton.background = selectedDrawable
        otherButton.background = defaultDrawable
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        otherButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter
        viewModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            wallpaperList.clear()
            wallpaperList.addAll(wallpapers)
            adapter.notifyDataSetChanged()
            hideSpinner()
            notFoundTextView.visibility = if (wallpaperList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun initRecyclerViewForCat() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapterCat
        viewCatModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            wallpaperCatList.clear()
            wallpaperCatList.addAll(wallpapers)
            adapter.notifyDataSetChanged()
            hideSpinner()
            notFoundTextView.visibility = if (wallpaperCatList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        if (x1 == 0f && y1 == 0f) return

        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) > MINI_DISTANCE && abs(deltaY) < MINI_DISTANCE) {
            if (deltaX < 0) {
                navController?.let { NavigationHandler.navigateToDestination(it, R.id.icon_changer) }
            }
        }

        x1 = 0f
        y1 = 0f
    }

    override fun onResume() {
        super.onResume()
        recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
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

    private fun loadAllWallpapers() {
        showSpinner()
        context?.let { nonNullContext ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val categoryWallpapers = listOf(
                        "S24" to listOf("s24_cover", "s24_1",  "s24_2","s24_3", "s24_4", "s24_5", "s24_6", "s24_7", "s24_8", "s24_9"),
                        "S25" to listOf("s25_cover", "s25_1",  "s25_2","s25_3", "s25_4", "s25_5", "s25_6", "s25_7", "s25_8", "s25_9"),
                        "S25 Ultra" to listOf("s25_ultra_cover", "s25_ultra_1",  "s25_ultra_2","s25_ultra_3", "s25_ultra_4", "s25_ultra_5", "s25_ultra_6", "s25_ultra_7", "s25_ultra_8", "s25_ultra_9"),
                        "Colors" to listOf("colors_cover", "colors_1",  "colors_2","colors_3", "colors_4", "colors_5", "colors_6", "colors_7", "colors_8", "colors_9"),
                    )

                    val allWallpapers = mutableListOf<WallpaperDetailItems>()
                    categoryWallpapers.forEach { (categoryTitle, drawableNames) ->
                        drawableNames.forEach { drawableName ->
                            val resourceId = nonNullContext.resources.getIdentifier(
                                drawableName,
                                "drawable",
                                nonNullContext.packageName
                            )
                            if (resourceId != 0) {
                                allWallpapers.add(WallpaperDetailItems(resourceId.toString()))
                            } else {
                                Log.w("loadAllWallpapers", "Drawable not found: $drawableName")
                            }
                        }
                    }

                    if (allWallpapers.isEmpty()) {
                        notFoundTextView.visibility = View.VISIBLE
                        notFoundTextView.text = "No wallpapers found."
                    } else {
                        viewModel.setWallpapers(allWallpapers)
                        notFoundTextView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    notFoundTextView.visibility = View.VISIBLE
                    notFoundTextView.text = "Error: ${e.message}"
                } finally {
                    hideSpinner()
                }
            }
        } ?: run {
            notFoundTextView.visibility = View.VISIBLE
            notFoundTextView.text = "Error: Context is null."
            hideSpinner()
        }
    }

    private fun loadCatWallpapers() {
        context?.let { nonNullContext ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val wallpaperList = listOf(
                        CatWallpaperItem("S24", "s24_cover"),
                        CatWallpaperItem("S25", "s25_cover"),
                        CatWallpaperItem("S25 Ultra", "s25_ultra_cover"),
                        CatWallpaperItem("Colors", "colors_cover"),
                    )

                    val wallpapers = mutableListOf<CatWallpaperItem>()

                    for (wallpaper in wallpaperList) {
                        try {
                            val resourceId = nonNullContext.resources.getIdentifier(
                                wallpaper.imageUrl,
                                "drawable",
                                nonNullContext.packageName
                            )
                            if (resourceId != 0) {
                                wallpapers.add(CatWallpaperItem(wallpaper.title, resourceId.toString()))
                            }
                        } catch (e: Exception) {
                            Log.e("LoadWallpapers", "Error processing wallpaper: ${wallpaper.imageUrl}", e)
                        }
                    }

                    if (wallpapers.isEmpty()) {
                        notFoundTextView.visibility = View.VISIBLE
                        notFoundTextView.text = "No wallpapers found."
                    } else {
                        viewCatModel.setWallpapers(wallpapers)
                        notFoundTextView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    notFoundTextView.visibility = View.VISIBLE
                    notFoundTextView.text = "Error: ${e.message}"
                    Log.e("LoadWallpapers", "Error loading wallpapers", e)
                } finally {
                    hideSpinner()
                }
            }
        } ?: run {
            notFoundTextView.visibility = View.VISIBLE
            notFoundTextView.text = "Error: Context is null."
            hideSpinner()
        }
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
