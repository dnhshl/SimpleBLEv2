package com.example.simpleblev2.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val TAG = "MainViewModel"

    private var _selectedDevice = MutableLiveData<String>("")
    val selectedDevice: LiveData<String>
        get() = _selectedDevice

    fun setSelectedDevice(device:String = "") {
        _selectedDevice.value = device
    }

    fun getMAC(): String {
        if (_selectedDevice.value!!.isEmpty()) return ""
        val device = _selectedDevice.value
        return device!!.substring(device.length-17)
    }

}