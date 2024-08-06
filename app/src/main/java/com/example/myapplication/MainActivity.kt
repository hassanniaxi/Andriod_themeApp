package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        // Set up BottomNavigationView with NavController
        val bottomNav = findViewById<BottomNavigationView>(R.id.nav_bottom)
        bottomNav.setupWithNavController(navController)

        // Set up listener to update header title on navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateHeaderTitle(destination.id)
        }
    }

    private fun updateHeaderTitle(destinationId: Int) {
        val title = when (destinationId) {
            R.id.home -> getString(R.string.fragment_home_title)
            R.id.profile -> getString(R.string.fragment_profile_title)
            R.id.wallpaper -> getString(R.string.fragment_wallpaper_title)
            R.id.theme -> getString(R.string.fragment_theme_title)

            else -> getString(R.string.app_name)
        }
        binding.headerTitle.text = title
    }
}
