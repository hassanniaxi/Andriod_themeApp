package com.example.myapplication

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    var isSearchViewExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize NavController
        navController = findNavController(R.id.nav_host_fragment)

        // Set up BottomNavigationView with NavController
        val bottomNav = findViewById<BottomNavigationView>(R.id.nav_bottom)
        bottomNav.setupWithNavController(navController)

        // Set up listener to update header title and manage ImageButton visibility on navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateHeaderTitle(destination.id)
            manageImageButtonVisibility(destination.id)
        }

        // Handle search button click
        binding.ringtoneSearchButton.setOnClickListener {
            toggleSearchView()
        }

        // Handle search view close button click
        binding.closeSearchButton.setOnClickListener {
            collapseSearchView()
        }
    }

    // Moved out of onCreate
    fun getSearchView(): SearchView {
        return binding.ringtoneSearchView
    }

    private fun toggleSearchView() {
        if (!isSearchViewExpanded) {
            expandSearchView()
        } else {
            collapseSearchView()
        }
    }

    private fun expandSearchView() {
        if (!isSearchViewExpanded) {
            isSearchViewExpanded = true

            // Show the search view and other related views
            binding.ringtoneSearchView.visibility = View.VISIBLE
            binding.closeSearchButton.visibility = View.VISIBLE
            binding.ringtoneSearchView.requestFocus()

            // Animate the width expansion
            val layoutParams = binding.ringtoneSearchView.layoutParams as ConstraintLayout.LayoutParams
            val startWidth = binding.ringtoneSearchView.width
            val endWidth = ConstraintLayout.LayoutParams.MATCH_PARENT
            val animator = ValueAnimator.ofInt(startWidth, endWidth)
            animator.duration = 300
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                layoutParams.width = value
                binding.ringtoneSearchView.layoutParams = layoutParams
            }
            animator.start()
        }
    }

    private fun collapseSearchView() {
        if (isSearchViewExpanded) {
            isSearchViewExpanded = false

            // Animate the width collapse
            val layoutParams = binding.ringtoneSearchView.layoutParams as ConstraintLayout.LayoutParams
            val startWidth = binding.ringtoneSearchView.width
            val endWidth = 0 // Adjust according to the initial size or desired collapse size
            val animator = ValueAnimator.ofInt(startWidth, endWidth)
            animator.duration = 300
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                layoutParams.width = value
                binding.ringtoneSearchView.layoutParams = layoutParams
            }
            animator.addListener(onEnd = {
                // Hide the search view and related views after animation completes
                binding.ringtoneSearchView.visibility = View.GONE
                binding.closeSearchButton.visibility = View.GONE
                binding.ringtoneSearchView.isIconified = true
            })
            animator.start()
        }
    }

    private fun updateHeaderTitle(destinationId: Int) {
        val title = when (destinationId) {
            R.id.home -> getString(R.string.fragment_home_title)
            R.id.wallpaper -> getString(R.string.fragment_wallpaper_title)
            R.id.ringtone -> getString(R.string.fragment_ringtone_title)
            else -> getString(R.string.app_name)
        }
        binding.headerTitle.text = title
    }

    private fun manageImageButtonVisibility(destinationId: Int) {
        if (destinationId == R.id.ringtone) {
            binding.ringtoneSearchButton.visibility = View.VISIBLE
        } else {
            binding.ringtoneSearchButton.visibility = View.GONE
            // Collapse search view if navigating away from ringtone fragment
            if (isSearchViewExpanded) {
                collapseSearchView()
            }
        }
    }
}
