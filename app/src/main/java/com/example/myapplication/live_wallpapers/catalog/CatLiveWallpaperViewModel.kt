package com.example.myapplication.live_wallpapers.catalog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CatLiveWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<CatLiveWallpaperItem>>()
    val wallpapers: LiveData<List<CatLiveWallpaperItem>> get() = _wallpapers

    fun setLiveWallpapers(wallpaperList: List<CatLiveWallpaperItem>) {
        _wallpapers.value = wallpaperList
    }
}
