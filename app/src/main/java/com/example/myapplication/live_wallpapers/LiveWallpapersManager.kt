package com.example.myapplication.live_wallpapers

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
import com.example.myapplication.databinding.FragmentLiveWallpapersBinding
import com.example.myapplication.live_wallpapers.all.AllLiveWallpaperViewModel
import com.example.myapplication.live_wallpapers.all.AllLiveWallpapersAdapter
import com.example.myapplication.live_wallpapers.catalog.CatLiveWallpaperAdapter
import com.example.myapplication.live_wallpapers.catalog.CatLiveWallpaperItem
import com.example.myapplication.live_wallpapers.catalog.CatLiveWallpaperViewModel
import com.example.myapplication.wallpaper.WallpaperDetailItems
import com.example.myapplication.wallpaper.category.CatWallpaperItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class LiveWallpapersManager : Fragment(), GestureDetector.OnGestureListener{

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllLiveWallpapersAdapter
    private val wallpaperList = mutableListOf<LiveWallpaperItems>()
    private lateinit var db: FirebaseFirestore
    private lateinit var notFoundTextView: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var viewModel: AllLiveWallpaperViewModel
    private var recyclerViewState: Parcelable? = null
    private val wallpaperCatList = mutableListOf<CatLiveWallpaperItem>()
    private lateinit var gestureDetector: GestureDetector
    private lateinit var binding: FragmentLiveWallpapersBinding
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f
    private var navController: NavController? = null

    private lateinit var adapterCat: CatLiveWallpaperAdapter
    private lateinit var viewCatModel: CatLiveWallpaperViewModel

    companion object {
        const val MINI_DISTANCE = 50
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLiveWallpapersBinding.inflate(inflater, container, false)
        val view = binding.root
        recyclerView = view.findViewById(R.id.wallpaper_recycler_view)
        spinner = view.findViewById(R.id.spinner)
        notFoundTextView = view.findViewById(R.id.not_found_text_view2)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        navController = findNavController()
        gestureDetector = GestureDetector(requireContext(), this)

        viewModel = ViewModelProvider(this).get(AllLiveWallpaperViewModel::class.java)
        adapter = AllLiveWallpapersAdapter(requireContext(), wallpaperList,findNavController())

        adapterCat = CatLiveWallpaperAdapter(requireContext(), wallpaperCatList, findNavController())
        viewCatModel = ViewModelProvider(this).get(CatLiveWallpaperViewModel::class.java)

        // default
        initRecyclerViewForCat()
        if (wallpaperCatList.isEmpty()) {
            showSpinner()
            loadCatWallpapers()
        }
        setFilterButtonState(binding.liveWallpaperCategory, binding.allLiveWallpapers)

        binding.allLiveWallpapers.setOnClickListener{
            setFilterButtonState(binding.allLiveWallpapers, binding.liveWallpaperCategory)
            initRecyclerView()
            if (wallpaperList.isEmpty()) {
                showSpinner()
                loadAllWallpapers()
            }
        }

        binding.liveWallpaperCategory.setOnClickListener{
            setFilterButtonState(binding.liveWallpaperCategory, binding.allLiveWallpapers)
            initRecyclerViewForCat()
            if (wallpaperCatList.isEmpty()) {
                showSpinner()
                loadCatWallpapers()
            }
        }

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            false
        }
        swipeRefreshLayout.setOnRefreshListener {
            loadAllWallpapers()
        }
        toTransfer()
        return view
    }

    private  fun toTransfer(){
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Custom redirection logic when the back button is pressed
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.live_wallpapers) {
//                        navController.navigate(R.id.anotherFragment)
//                        navController.popBackStack()
                        requireActivity().finish()
                    } else {
                        // Default behavior: go back
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
        if (x1 == 0f && y1 == 0f) return // Check initial values

        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) > MINI_DISTANCE && abs(deltaY) < MINI_DISTANCE) {
            if (deltaX < 0) {
                navController?.let { NavigationHandler.navigateToDestination(it, R.id.ringtones) }
            }else{
                navController?.let { NavigationHandler.navigateToDestination(it, R.id.wallpapers) }
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
                        LiveWallpaperItems("wall")
                    )

                    val allWallpapers = mutableListOf<LiveWallpaperItems>()
                    for (wallpaper in categoryWallpapers) {
                        try {
                            val resourceId = nonNullContext.resources.getIdentifier(
                                wallpaper.imageUrl,
                                "raw",
                                nonNullContext.packageName
                            )
                            if (resourceId != 0) {
                                allWallpapers.add(LiveWallpaperItems(resourceId.toString()))
                            }
                        } catch (e: Exception) {
                            Log.e("LoadWallpapers", "Error processing wallpaper: ${wallpaper.imageUrl}", e)
                        }
                    }

                    if (allWallpapers.isEmpty()) {
                        notFoundTextView.visibility = View.VISIBLE
                        notFoundTextView.text = "No wallpapers found."
                    } else {
                        viewModel.setLiveWallpapers(allWallpapers)
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
                        CatWallpaperItem("Sea View", "wall"),
                    )

                    val wallpapers = mutableListOf<CatLiveWallpaperItem>()

                    for (wallpaper in wallpaperList) {
                        try {
                            val resourceId = nonNullContext.resources.getIdentifier(
                                wallpaper.imageUrl,
                                "raw",
                                nonNullContext.packageName
                            )
                            if (resourceId != 0) {
                                wallpapers.add(CatLiveWallpaperItem(wallpaper.title, resourceId.toString()))
                            }
                        } catch (e: Exception) {
                            Log.e("LoadWallpapers", "Error processing wallpaper: ${wallpaper.imageUrl}", e)
                        }
                    }

                    if (wallpapers.isEmpty()) {
                        notFoundTextView.visibility = View.VISIBLE
                        notFoundTextView.text = "No wallpapers found."
                    } else {
                        viewCatModel.setLiveWallpapers(wallpapers)
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
