package com.example.myapplication.wallpaper.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CatWallpaperViewModel : ViewModel() {
    private val _wallpapers = MutableLiveData<List<CatWallpaperItem>>()
    val wallpapers: LiveData<List<CatWallpaperItem>> get() = _wallpapers

    fun setWallpapers(wallpaperList: List<CatWallpaperItem>) {
        _wallpapers.value = wallpaperList
    }
}
