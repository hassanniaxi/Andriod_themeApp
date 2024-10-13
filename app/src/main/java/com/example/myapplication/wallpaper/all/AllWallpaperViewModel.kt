package com.example.myapplication.wallpaper.all

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.wallpaper.WallpaperDetailItems

class AllWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<WallpaperDetailItems>>()
    val wallpapers: LiveData<List<WallpaperDetailItems>> get() = _wallpapers

    fun setWallpapers(wallpaperList: List<WallpaperDetailItems>) {
        _wallpapers.value = wallpaperList
    }
}
