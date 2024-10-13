package com.example.myapplication.ringtone

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RingtoneViewModel : ViewModel() {
    private val _ringtones = MutableLiveData<List<RingtoneItem>>()
    val ringtones: LiveData<List<RingtoneItem>> = _ringtones

    fun setRingtones(ringtones: List<RingtoneItem>) {
        _ringtones.value = ringtones
    }
}