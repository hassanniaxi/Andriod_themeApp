package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding

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
        binding.navBottom.setupWithNavController(navController)

        binding.navBottom.setOnItemSelectedListener { item ->
            NavigationHandler.navigateToDestination(navController, item.itemId)
            true
        }
    }
}