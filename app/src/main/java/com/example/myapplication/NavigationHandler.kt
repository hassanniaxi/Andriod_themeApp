package com.example.myapplication

import androidx.navigation.NavController

object NavigationHandler {
    fun navigateToDestination(navController: NavController, destinationId: Int) {
        when (destinationId) {
            R.id.ringtones -> navController.navigate(R.id.ringtones)
            R.id.wallpapers -> navController.navigate(R.id.wallpapers)
            R.id.icon_changer -> navController.navigate(R.id.icon_changer)
            R.id.preview -> navController.navigate(R.id.preview)
        }
    }
}