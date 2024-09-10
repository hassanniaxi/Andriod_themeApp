package com.example.myapplication.live_wallpapers.all

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.live_wallpapers.LiveWallpaperItems

class AllLiveWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<LiveWallpaperItems>>()
    val wallpapers: LiveData<List<LiveWallpaperItems>> get() = _wallpapers

    fun setLiveWallpapers(wallpaperList: List<LiveWallpaperItems>) {
        _wallpapers.value = wallpaperList
    }
}
