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
                        "S24" to listOf("s24_cover", "s24_1",  "s24_2","s24_3", "s24_4", "s24_5", "s24_6", "s24_7", "s24_8", "s24_9"),
                        "S25" to listOf("s25_cover", "s25_1",  "s25_2","s25_3", "s25_4", "s25_5", "s25_6", "s25_7", "s25_8", "s25_9"),
                        "S25 Ultra" to listOf("s25_ultra_cover", "s25_ultra_1",  "s25_ultra_2","s25_ultra_3", "s25_ultra_4", "s25_ultra_5", "s25_ultra_6", "s25_ultra_7", "s25_ultra_8", "s25_ultra_9"),
                        "Colors" to listOf("colors_cover", "colors_1",  "colors_2","colors_3", "colors_4", "colors_5", "colors_6", "colors_7", "colors_8", "colors_9"),
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