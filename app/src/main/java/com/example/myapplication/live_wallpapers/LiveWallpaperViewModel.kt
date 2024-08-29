package com.example.myapplication.live_wallpapers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LiveWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<LiveWallpaperlItems>>()
    val wallpapers: LiveData<List<LiveWallpaperlItems>> get() = _wallpapers

    fun setWallpapers(wallpaperList: List<LiveWallpaperlItems>) {
        _wallpapers.value = wallpaperList
    }
}
