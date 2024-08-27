package com.example.myapplication.wallpaper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityShowWallpaperBinding

class ShowWallpaper : AppCompatActivity() {

    private lateinit var binding: ActivityShowWallpaperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val wallpaperUrl = intent.getStringExtra(SHOW_WALLPAPER)

        Glide.with(this).load(wallpaperUrl).error(com.example.myapplication.R.drawable.baseline_error_outline_24).into(binding.showWallpaper)

    }

    companion object {
        const val SHOW_WALLPAPER = "show_wallpaper"
    }
}