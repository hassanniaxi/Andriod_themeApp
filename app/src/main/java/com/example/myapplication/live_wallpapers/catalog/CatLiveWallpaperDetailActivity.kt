package com.example.myapplication.live_wallpapers.catalog

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityWallpaperDetailBinding
import com.example.myapplication.live_wallpapers.LiveWallpaperItems
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CatLiveWallpaperDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WALLPAPER_TITLE = "extra_wallpaper_title"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CatLiveWallpaperDetailAdapter
    private val wallpaperDetailList = mutableListOf<LiveWallpaperItems>()
    private lateinit var db: FirebaseFirestore
    private lateinit var spinner: ProgressBar
    private lateinit var notFoundTextView: TextView
    private lateinit var binding: ActivityWallpaperDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWallpaperDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val wallpaperTitle = intent.getStringExtra(EXTRA_WALLPAPER_TITLE)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        spinner = findViewById(R.id.spinner)
        notFoundTextView = findViewById(R.id.not_found_text_view)
        adapter = CatLiveWallpaperDetailAdapter(this, wallpaperDetailList)
        recyclerView.adapter = adapter

        showSpinner()

        db = FirebaseFirestore.getInstance()
        loadWallpaperImages(wallpaperTitle)

        binding.backToWallpapers.setOnClickListener{
            finish()
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
    }
    private fun loadWallpaperImages(title: String?) {
        if (title == null) return

        // Start by showing the spinner
        showSpinner()
        this?.let { nonNullContext ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val drawableMap = mapOf(
                        "Sea View" to listOf("water"),
                    )
                    val drawableNames = drawableMap[title] ?: emptyList()

                    wallpaperDetailList.clear()
                    drawableNames.forEach { drawableName ->
                        val resourceId = nonNullContext.resources.getIdentifier(
                            drawableName,
                            "raw",
                            nonNullContext.packageName
                        )
                        if (resourceId != 0) {
                            wallpaperDetailList.add(LiveWallpaperItems(resourceId.toString()))
                        }
                    }

                    adapter.notifyDataSetChanged()
                    hideSpinner()
                    updateNotFoundMessage()
                } catch (e: Exception) {
                    hideSpinner()
                    notFoundTextView.visibility = View.VISIBLE
                    notFoundTextView.text = if (wallpaperDetailList.isEmpty()) {
                        "Wallpapers not found"
                    } else {
                        "Error: ${e.message}"
                    }
                }
            }
        }
    }
    private fun updateNotFoundMessage() {
        notFoundTextView.text =  "Wallpapers not found"
        notFoundTextView.visibility = if (adapter.itemCount == 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

}