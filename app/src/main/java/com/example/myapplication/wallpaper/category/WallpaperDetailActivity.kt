package com.example.myapplication.wallpaper.category

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityWallpaperDetailBinding
import com.example.myapplication.wallpaper.WallpaperDetailItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WallpaperDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WALLPAPER_TITLE = "extra_wallpaper_title"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WallpaperDetailAdapter
    private val wallpaperDetailList = mutableListOf<WallpaperDetailItems>()
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
        adapter = WallpaperDetailAdapter(this, wallpaperDetailList)
        recyclerView.adapter = adapter

        showSpinner()

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

        showSpinner()
        this?.let { nonNullContext ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val drawableMap = mapOf(
                        "Flowers" to listOf("flower_cover", "flower_1",  "flower_3", "flower_4", "flower_5", "flower_6", "flower_7", "flower_8", "flower_9", "flower_10"),
                        "Cars" to listOf("car_cover", "car_1", "car_2", "car_3", "car_6", "car_7", "car_8", "car_9", "car_10"),
                        "Nature" to listOf("nature_cover", "nature_1", "nature_2", "nature_3",  "nature_10"),
                        "Vintage" to listOf("vintage_cover", "vintage_1", "vintage_2", "vintage_3", "vintage_4", "vintage_6", "vintage_7", "vintage_8"),
                        "Space" to listOf("space_cover",  "space_4",  "space_7", "space_8", "space_9", "space_10"),
                        "Animals" to listOf("animal_cover", "animal_1", "animal_3",  "animal_5", "animal_6", "animal_7", "animal_9", "animal_10"),
                        "Fashion" to listOf("fashion_cover", "fashion_1", "fashion_3", "fashion_4", "fashion_5", "fashion_6", "fashion_7", "fashion_8", "fashion_9", "fashion_10")
                    )

                    val drawableNames = drawableMap[title] ?: emptyList()

                    wallpaperDetailList.clear()
                    drawableNames.forEach { drawableName ->
                        val resourceId = nonNullContext.resources.getIdentifier(
                            drawableName,
                            "drawable",
                            nonNullContext.packageName
                        )
                        if (resourceId != 0) {
                            wallpaperDetailList.add(WallpaperDetailItems(resourceId.toString()))
                        }
                    }

                    // Notify the adapter of data changes and update UI
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