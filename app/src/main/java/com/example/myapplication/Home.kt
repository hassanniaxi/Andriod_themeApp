package com.example.myapplication

import android.annotation.SuppressLint
import android.graphics.Color
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.ringtone.NavigationHandler
import com.example.myapplication.ringtone.Ringtone
import com.example.myapplication.ringtone.Ringtone.Companion
import com.example.myapplication.wallpaper.WallpaperDetailItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class Home : Fragment(), GestureDetector.OnGestureListener{

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllWallpaperAdapter
    private val wallpaperList = mutableListOf<AllWallpaperDetailItem>()
    private lateinit var db: FirebaseFirestore
    private lateinit var notFoundTextView: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var viewModel: AllWallpaperViewModel
    private var recyclerViewState: Parcelable? = null
    private lateinit var gestureDetector: GestureDetector
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f
    private var navController: NavController? = null

    companion object {
        const val MINI_DISTANCE = 150
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.wallpaper_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        spinner = view.findViewById(R.id.spinner)
        notFoundTextView = view.findViewById(R.id.not_found_text_view2)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        navController = findNavController()
        gestureDetector = GestureDetector(requireContext(), this)

        adapter = AllWallpaperAdapter(requireContext(), wallpaperList)
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this).get(AllWallpaperViewModel::class.java)

        viewModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            wallpaperList.clear()
            wallpaperList.addAll(wallpapers)
            adapter.notifyDataSetChanged()
            hideSpinner()
            notFoundTextView.visibility = if (wallpaperList.isEmpty()) View.VISIBLE else View.GONE
        }

        if (savedInstanceState != null) {
            recyclerViewState = savedInstanceState.getParcelable("recyclerViewState")
        }


        swipeRefreshLayout.setOnRefreshListener {
            loadWallpapers()
        }

        if (wallpaperList.isEmpty()) {
            showSpinner()
            loadWallpapers()
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


        return view
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        if (x1 == 0f && y1 == 0f) return // Check initial values

        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) > Ringtone.MINI_DISTANCE && abs(deltaY) < Ringtone.MINI_DISTANCE) {
            if (deltaX > 0) {

            } else {
                navController?.let { NavigationHandler.navigateToDestination(it, R.id.ringtone) }
            }
        }

        x1 = 0f
        y1 = 0f
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::recyclerView.isInitialized) {
            outState.putParcelable("recyclerViewState", recyclerView.layoutManager?.onSaveInstanceState())
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            recyclerViewState = savedInstanceState.getParcelable("recyclerViewState")
        }
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

    private fun loadWallpapers() {
        db = FirebaseFirestore.getInstance()

        // Fetch wallpapers from the main collection
        db.collection("wallpapers").get()
            .addOnSuccessListener { result ->
                val wallpapers = mutableListOf<AllWallpaperDetailItem>()

                // Loop through the main collection documents
                for (document in result) {
                    val imageUrl = document.getString("Cover") ?: ""
                    if (imageUrl.isNotEmpty()) {
                        wallpapers.add(AllWallpaperDetailItem(imageUrl))
                    }

                    // Fetch wallpapers from the sub-collection for each document
                    db.collection("wallpapers").document(document.id).collection("detail")
                        .get()
                        .addOnSuccessListener { subResult ->
                            for (subDocument in subResult) {
                                val subImageUrl = subDocument.getString("url") ?: ""
                                if (subImageUrl.isNotEmpty()) {
                                    wallpapers.add(AllWallpaperDetailItem(subImageUrl))
                                }
                            }

                            // Update UI after fetching sub-collection data
                            if (wallpapers.isEmpty()) {
                                notFoundTextView.visibility = View.VISIBLE
                            } else {
                                viewModel.setWallpapers(wallpapers)
                            }
                            hideSpinner()
                        }
                        .addOnFailureListener { subException ->
                            hideSpinner()
                            notFoundTextView.visibility = View.VISIBLE
                            notFoundTextView.text = "Error loading sub-collection: ${subException.message}"
                        }
                }
            }
            .addOnFailureListener { exception ->
                hideSpinner()
                notFoundTextView.visibility = View.VISIBLE
                notFoundTextView.text = "Error: ${exception.message}"
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
