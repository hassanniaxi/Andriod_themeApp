package com.example.myapplication.ringtone

import android.util.Log
import androidx.navigation.NavController
import com.example.myapplication.R

object NavigationHandler {
    fun navigateToDestination(navController: NavController, destinationId: Int) {
        Log.d("desti", "navigateToDestination: ${destinationId}")
        Log.d("desti", "naviId: ${navController}")
        when (destinationId) {
            R.id.ringtone -> navController.navigate(R.id.ringtone)
            R.id.home -> navController.navigate(R.id.home)
            R.id.wallpaper -> navController.navigate(R.id.wallpaper)
        }
    }
}