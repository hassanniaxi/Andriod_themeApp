package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = Intent(this, FragmentManagement::class.java).apply {
            putExtra(FragmentManagement.NAV_ID, R.id.ringtones)
        }
        startActivity(intent)
        //setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.ringtones.setOnClickListener {
            startFragmentManagementActivity(R.id.ringtones)
        }
        binding.liveWall.setOnClickListener {
            startFragmentManagementActivity(R.id.live_wallpapers)
        }
        binding.allWallpapers.setOnClickListener {
            startFragmentManagementActivity(R.id.wallpapers)
        }
        binding.wallpaperCat.setOnClickListener {
            startFragmentManagementActivity(R.id.wallpaper_category)
        }
    }

    private fun startFragmentManagementActivity(navId: Int) {
        val intent = Intent(this, FragmentManagement::class.java).apply {
            putExtra(FragmentManagement.NAV_ID, navId)
        }
        startActivity(intent)
    }
}
