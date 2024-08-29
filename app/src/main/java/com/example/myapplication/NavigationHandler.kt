package com.example.myapplication

import androidx.navigation.NavController

object NavigationHandler {
    fun navigateToDestination(navController: NavController, destinationId: Int) {
        when (destinationId) {
            R.id.ringtones -> navController.navigate(R.id.ringtones)
            R.id.wallpapers -> navController.navigate(R.id.wallpapers)
            R.id.wallpaper_category -> navController.navigate(R.id.wallpaper_category)
            R.id.live_wallpapers -> navController.navigate(R.id.live_wallpapers)
        }
    }
}