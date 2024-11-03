package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import android.content.Intent
import android.content.res.Configuration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.ringtones.setOnClickListener {
            startFragmentManagementActivity(R.id.ringtones)
        }
        binding.iconChanger.setOnClickListener {
            startFragmentManagementActivity(R.id.icon_changer)
        }
        binding.allWallpapers.setOnClickListener {
            startFragmentManagementActivity(R.id.wallpapers)
        }
        binding.preview.setOnClickListener {
            startFragmentManagementActivity(R.id.preview)
        }
        binding.catWallpapers.setOnClickListener {
            startFragmentManagementActivity(R.id.wallpapers)
        }
        binding.rateUs.setOnClickListener {
            // write your code here
        }
    }

    private fun startFragmentManagementActivity(navId: Int) {
        val intent = Intent(this, FragmentManagement::class.java).apply {
            putExtra(FragmentManagement.NAV_ID, navId)
        }
        startActivity(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

}