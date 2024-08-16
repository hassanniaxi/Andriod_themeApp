package com.example.myapplication.wallpaper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<WallpaperItem>>()
    val wallpapers: LiveData<List<WallpaperItem>> get() = _wallpapers

    fun setWallpapers(wallpaperList: List<WallpaperItem>) {
        _wallpapers.value = wallpaperList
    }
}
