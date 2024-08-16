package com.example.myapplication.wallpaper

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ActivityRingtoneDetailBinding
import com.example.myapplication.databinding.ActivityWallpaperDetailBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class WallpaperDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WALLPAPER_TITLE = "extra_wallpaper_title"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WallpaperDetailAdapter
    private val wallpaperDetailList = mutableListOf<WallpaperDetailItem>()
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
        adapter = WallpaperDetailAdapter(this, wallpaperDetailList)
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

        db.collection("wallpapers").document(title.lowercase()).collection("wallpapers")
            .get()
            .addOnSuccessListener { subResult ->
                wallpaperDetailList.clear()
                for (subDocument in subResult) {
                    val imageUrl = subDocument.getString("url") ?: ""
                    wallpaperDetailList.add(WallpaperDetailItem(imageUrl))
                }
                adapter.notifyDataSetChanged()
                hideSpinner()
                updateNotFoundMessage()
            }
            .addOnFailureListener { exception ->
                hideSpinner()
                notFoundTextView.visibility = View.VISIBLE
                notFoundTextView.text = if (wallpaperDetailList.isEmpty()) {
                    "Wallpapers not found"
                } else {
                    "Error: ${exception.message}"
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