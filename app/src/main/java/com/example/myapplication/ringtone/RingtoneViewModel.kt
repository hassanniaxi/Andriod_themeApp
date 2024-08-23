package com.example.myapplication.ringtone
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel


class RingtoneViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _ringtones = savedStateHandle.getLiveData<List<RingtoneItem>>("ringtones", emptyList())
    val ringtones: LiveData<List<RingtoneItem>> = _ringtones

    fun setRingtones(ringtones: List<RingtoneItem>) {
        savedStateHandle["ringtones"] = ringtones
    }

    // Add a method to check if data is already loaded
    fun isDataLoaded(): Boolean {
        return _ringtones.value?.isNotEmpty() == true
    }
}


//class RingtoneViewModel : ViewModel() {
//    private val _ringtones = MutableLiveData<List<RingtoneItem>>()
//    val ringtones: LiveData<List<RingtoneItem>> get() = _ringtones
//
//    fun setRingtones(ringtoneList: List<RingtoneItem>) {
//        _ringtones.value = ringtoneList
//    }
//}