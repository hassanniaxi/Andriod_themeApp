package com.example.myapplication.live_wallpapers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LiveWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<LiveWallpaperItems>>()
    val wallpapers: LiveData<List<LiveWallpaperItems>> get() = _wallpapers

    fun setWallpapers(wallpaperList: List<LiveWallpaperItems>) {
        _wallpapers.value = wallpaperList
    }
}
