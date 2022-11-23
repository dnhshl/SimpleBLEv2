package com.example.simpleblev2.model


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuidFrom
import com.juul.kable.Advertisement
import com.juul.kable.Filter
import com.juul.kable.Scanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit


private val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10)

sealed class ScanStatus {
    object Stopped : ScanStatus()
    object Scanning : ScanStatus()
    data class Failed(val message: CharSequence) : ScanStatus()
}

data class Device(
    val name: String,
    val address: String
) {
    override fun toString(): String = name + ": " + address
}

class MainViewModel : ViewModel() {
    private val scanner = Scanner {
        filters = listOf(
            Filter.Service(uuidFrom("0000FFE0-0000-1000-8000-00805F9B34FB"))
        )
    }
    private val scanScope = viewModelScope.childScope()
    private val found = hashMapOf<String, Advertisement>()

    private val _status = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)
    val status = _status.asStateFlow()

    private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    val advertisements = _advertisements.asStateFlow()


    private val _deviceList = MutableLiveData<MutableList<Device>>()
    val deviceList: LiveData<MutableList<Device>>
        get() = _deviceList


    private var deviceSelected = ""

    fun getDeviceList(): List<Device>? {
        return _deviceList.value
    }
    fun setDeviceSelected(devicestring: String) {
        deviceSelected = devicestring
    }
    fun getDeviceSelected(): String {
        return deviceSelected
    }


    init {
        _deviceList.value = mutableListOf()
    }

    fun startScan() {
        if (_status.value == ScanStatus.Scanning) return // Scan already in progress.
        _status.value = ScanStatus.Scanning

        scanScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause -> _status.value = ScanStatus.Failed(cause.message ?: "Unknown error") }
                    .onCompletion { cause -> if (cause == null || cause is CancellationException) _status.value =
                        ScanStatus.Stopped
                    }
                    //.filter { it.isSensorTag }
                    .collect { advertisement ->
                        val device = Device(name = advertisement.name.toString(),
                                        address = advertisement.address.toString())
                        if (_deviceList.value?.contains(device) == false) {
                            _deviceList.value?.add(device)
                            _deviceList.notifyObserver()
                        }
                        //found[advertisement.address] = advertisement
                        //_advertisements.value = found.values.toList()
                        //Log.i(">>>>>", advertisement.address.toString())
                        Log.i(">>>>", _deviceList.value.toString())
                        //Log.i(">>>", found.toString())
                    }
            }
        }
    }

    fun stopScan() {
        scanScope.cancelChildren()
    }

    fun clear() {
        stopScan()
        _advertisements.value = emptyList()
    }

    // Extension Function, um Änderung in den Einträgen von Listen
    // dem Observer anzeigen zu können
    fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

}