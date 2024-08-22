package com.example.myapplication.wallpaper

import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class Wallpaper : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WallpaperAdapter
    private val wallpaperList = mutableListOf<WallpaperItem>()
    private lateinit var db: FirebaseFirestore
    private lateinit var notFoundTextView: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var viewModel: WallpaperViewModel
    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallpaper, container, false)

        recyclerView = view.findViewById(R.id.wallpaper_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        spinner = view.findViewById(R.id.spinner)
        notFoundTextView = view.findViewById(R.id.not_found_text_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        adapter = WallpaperAdapter(requireContext(), wallpaperList)
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this).get(WallpaperViewModel::class.java)

        viewModel.wallpapers.observe(viewLifecycleOwner) { wallpapers ->
            wallpaperList.clear()
            wallpaperList.addAll(wallpapers)
            adapter.notifyDataSetChanged()
            hideSpinner()
            notFoundTextView.visibility = if (wallpaperList.isEmpty()) View.VISIBLE else View.GONE
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadWallpapers()
        }

        if (wallpaperList.isEmpty()) {
            showSpinner()
            loadWallpapers()
        }

        // Set up gesture detector
        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent, velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val SWIPE_MIN_DISTANCE = 120
                    val SWIPE_MAX_OFF_PATH = 250
                    val SWIPE_THRESHOLD_VELOCITY = 200
                    try {
                        if (e1 == null) return false
                        if (abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) return false
                        if (e1.x - e2.x > SWIPE_MIN_DISTANCE
                            && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                        ) {
                        } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE
                            && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                        ) {
                            findNavController().navigate(R.id.ringtone) // Swipe Right
                        }
                    } catch (e: Exception) {
                        // Handle exception
                    }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

        view.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        return view
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
        db.collection("wallpapers").get()
            .addOnSuccessListener { result ->
                val wallpapers = mutableListOf<WallpaperItem>()
                for (document in result) {
                    val title = document.getString("title") ?: "Unknown"
                    val imageUrl = document.getString("imageUrl") ?: ""
                    wallpapers.add(WallpaperItem(title, imageUrl))
                }
                viewModel.setWallpapers(wallpapers)
                if (wallpapers.isEmpty()) {
                    notFoundTextView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                hideSpinner()
                notFoundTextView.visibility = View.VISIBLE
                notFoundTextView.text = "Error: ${exception.message}"
            }
    }
}
