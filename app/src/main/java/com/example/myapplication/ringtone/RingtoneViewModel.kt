package com.example.myapplication.ringtone
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RingtoneViewModel : ViewModel() {
    private val _ringtones = MutableLiveData<List<RingtoneItem>>()
    val ringtones: LiveData<List<RingtoneItem>> get() = _ringtones

    fun setRingtones(ringtoneList: List<RingtoneItem>) {
        _ringtones.value = ringtoneList
    }
}