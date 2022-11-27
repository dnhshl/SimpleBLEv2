package com.example.simpleblev2.model


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuidFrom
import com.example.simpleblev2.esp32ble.CUSTOM_SERVICE_UUID
import com.example.simpleblev2.esp32ble.ConnectState
import com.example.simpleblev2.esp32ble.Esp32Ble
import com.example.simpleblev2.esp32ble.ScanState
import com.juul.kable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit



private val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10)



data class Device(
    val name: String,
    val address: String
) {
    override fun toString(): String = name + ": " + address
}




class MainViewModel : ViewModel() {


    private lateinit var peripheral: Peripheral
    private lateinit var esp32: Esp32Ble

    private val _deviceList = MutableLiveData<MutableList<Device>>()
    val deviceList: LiveData<MutableList<Device>>
        get() = _deviceList

    init {
        _deviceList.value = mutableListOf()
    }

    fun getDeviceList(): List<Device>? {
        return _deviceList.value
    }


    private var deviceSelected = ""
    fun getDeviceSelected(): String {
        return deviceSelected
    }


    fun setDeviceSelected(devicestring: String) {
        deviceSelected = devicestring
        val macAddress = devicestring.substring(devicestring.length -17);
        peripheral = viewModelScope.peripheral(macAddress) {
            onServicesDiscovered {
                requestMtu(517)
            }
        }
        esp32 = Esp32Ble(peripheral)

        viewModelScope.launch {
            peripheral.state.collect { state ->
                Log.i(">>>> Connection State:", state.toString())
                _connectState.value = state.toString()
            }
            esp32.data.collect { data ->
                     Log.i(">>>>> data:", data.toString())
            }
        }
    }

    // Scanning
    // ------------------------------------------------------------------------------

    private val scanner = Scanner {
        filters = listOf(
            Filter.Service(uuidFrom(CUSTOM_SERVICE_UUID))
        )
    }
    private val scanScope = viewModelScope.childScope()
    private val scanState = MutableStateFlow<ScanState>(ScanState.Stopped)

    fun startScan() {
        if (scanState.value == ScanState.Scanning) return // Scan already in progress.
        scanState.value = ScanState.Scanning

        scanScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause -> scanState.value = ScanState.Failed(cause.message ?: "Unknown error") }
                    .onCompletion { cause -> if (cause == null || cause is CancellationException) scanState.value =
                        ScanState.Stopped
                    }
                    .collect { advertisement ->
                        val device = Device(name = advertisement.name.toString(),
                                        address = advertisement.address.toString())
                        if (_deviceList.value?.contains(device) == false) {
                            _deviceList.value?.add(device)
                            _deviceList.notifyObserver()
                        }
                        Log.i(">>>>", _deviceList.value.toString())
                    }
            }
        }
    }

    fun stopScan() {
        scanScope.cancelChildren()
    }

    // Connecting
    // --------------------------------------------------------------------------


    private val _connectState = MutableLiveData<String>()
    val connectState: LiveData<String>
        get() = _connectState

    fun connect() {
        viewModelScope.launch { esp32.connect() }
    }

    fun disconnect() {
        viewModelScope.launch { esp32.disconnect() }
    }



    // Extension Function, um Änderung in den Einträgen von Listen
    // dem Observer anzeigen zu können
    fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

}

