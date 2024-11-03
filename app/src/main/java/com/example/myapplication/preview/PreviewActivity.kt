package com.example.myapplication.preview

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityPreviewBinding

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private lateinit var wallpaperUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            val applyingWallpaper = it.getStringExtra(PREVIEW_WALLPAPER)
            applyingWallpaper?.let { imageUrl ->
                wallpaperUri = Uri.parse("android.resource://${applicationContext.packageName}/${imageUrl}")

                Glide.with(this)
                    .load(wallpaperUri)
                    .placeholder(R.drawable.wallicon)
                    .error(R.drawable.baseline_error_outline_24)
                    .into(binding.toApplyWallpaper)
            }
        } ?: run {
            finish()
        }
    }

    companion object {
        const val PREVIEW_WALLPAPER = "apply_wallpaper"
    }
}
