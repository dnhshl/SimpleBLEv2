package com.example.simpleblev2.model


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuidFrom
import com.example.simpleblev2.esp32ble.*
import com.juul.kable.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit










class MainViewModel : ViewModel() {


    private lateinit var peripheral: Peripheral
    private lateinit var esp32: Esp32Ble
    private lateinit var dataLoadJob: Job
    private lateinit var scanJob: Job

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
        Log.i(">>>> services", peripheral.services.toString())

        viewModelScope.launch {
            peripheral.state.collect { state ->
                Log.i(">>>> Connection State:", state.toString())
                _connectState.value = state.toString()
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

    private val scanState = MutableStateFlow<ScanState>(ScanState.Stopped)

    fun startScan() {
        if (scanState.value == ScanState.Scanning) return // Scan already in progress.
        scanState.value = ScanState.Scanning

        val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10)
        scanJob = viewModelScope.launch {
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
        scanJob.cancel()
    }

    // Connecting
    // --------------------------------------------------------------------------


    private val _connectState = MutableLiveData<String>()
    val connectState: LiveData<String>
        get() = _connectState

    fun connect() {
        viewModelScope.launch {
            esp32.connect()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            // Allow 5 seconds for graceful disconnect before forcefully closing `Peripheral`.
            withTimeoutOrNull(5_000L) {
                esp32.disconnect()
            }
        }
    }



    // Extension Function, um Änderung in den Einträgen von Listen
    // dem Observer anzeigen zu können
    fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }


    // Communication
    // ____________________________________________________________________


    var ledData = LedData()

    private var _esp32Data = MutableLiveData<Esp32Data>(Esp32Data())
    val esp32Data: LiveData<Esp32Data>
        get() = _esp32Data

    fun startDataLoadJob() {
        dataLoadJob = viewModelScope.launch {
            esp32.incommingMessages.collect { msg ->
                val jsonstring = String(msg)
                Log.i(">>>> msg in", jsonstring)
                _esp32Data.value = jsonParseEsp32Data(jsonstring)
            }
        }
    }

    fun cancelDataLoadJob() {
        dataLoadJob.cancel()
    }

    fun sendLedData() {
        viewModelScope.launch {
            try {
                esp32.sendMessage(jsonEncodeLedData(ledData))
            } catch (e: Exception) {
                Log.i(">>>>>", "Error sending ledData ${e.message}" + e.toString())
            }
        }
    }

    private fun jsonEncodeLedData(ledData: LedData): String {
        val obj = JSONObject()
        obj.put("LED", ledData.led)
        obj.put("LEDBlinken", ledData.ledBlinken)
        return obj.toString()
    }

    fun jsonParseEsp32Data(jsonString: String): Esp32Data {
        try {
            val obj = JSONObject(jsonString)
            return Esp32Data(
                ledstatus = obj.getString("ledstatus"),
                potiArray = obj.getJSONArray("potiArray")
            )
        } catch (e: Exception) {
            Log.i(">>>>", "Error decoding JSON ${e.message}")
            return Esp32Data()
        }
    }

}

