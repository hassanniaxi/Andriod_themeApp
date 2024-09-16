package com.example.myapplication.icon_changer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IconsViewModel : ViewModel() {
    private val _icons = MutableLiveData<List<IconDetail>>()
    val icons: LiveData<List<IconDetail>> get() = _icons

    fun setIcons(iconList: List<IconDetail>) {
        _icons.value = iconList
    }
}
