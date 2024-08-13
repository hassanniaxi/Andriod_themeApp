package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize NavController
        navController = findNavController(R.id.nav_host_fragment)

        // Set up navigation
        findViewById<BottomNavigationView>(R.id.nav_bottom).setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateHeaderTitle(destination.id)
            if (destination.id == R.id.ringtone) {
                binding.ringtoneSearchButton.visibility = View.VISIBLE
            } else {
                binding.ringtoneSearchButton.visibility = View.GONE
                // Ensure the search view is collapsed
                (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? Ringtone)?.collapseSearchView()
            }
        }
    }

    fun getSearchView(): SearchView = binding.ringtoneSearchView
    fun getRingtoneSearchButton(): ImageButton = binding.ringtoneSearchButton

    private fun updateHeaderTitle(destinationId: Int) {
        val title = when (destinationId) {
            R.id.home -> getString(R.string.fragment_home_title)
            R.id.wallpaper -> getString(R.string.fragment_wallpaper_title)
            R.id.ringtone -> getString(R.string.fragment_ringtone_title)
            else -> getString(R.string.app_name)
        }
        binding.headerTitle.text = title
    }
}