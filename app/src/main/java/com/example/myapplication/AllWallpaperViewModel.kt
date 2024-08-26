package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AllWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<AllWallpaperDetailItem>>()
    val wallpapers: LiveData<List<AllWallpaperDetailItem>> get() = _wallpapers

    fun setWallpapers(wallpaperList: List<AllWallpaperDetailItem>) {
        _wallpapers.value = wallpaperList
    }
}
