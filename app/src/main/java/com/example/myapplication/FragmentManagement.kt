package com.example.myapplication

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityFragmentManagementBinding
import com.example.myapplication.databinding.ActivityMainBinding

class FragmentManagement : AppCompatActivity() {

    private lateinit var binding: ActivityFragmentManagementBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment)
        binding.navBottom.setupWithNavController(navController)

        binding.navBottom.setOnItemSelectedListener { item ->
            NavigationHandler.navigateToDestination(navController, item.itemId)
            true
        }

        val navId = intent.getIntExtra(NAV_ID, -1)
        if (navId != -1) {
            navController.navigate(navId)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object{
        const val NAV_ID  = "extra_nav_id"
    }
}