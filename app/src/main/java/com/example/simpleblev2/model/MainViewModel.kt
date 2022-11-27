package com.example.simpleblev2.model


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuidFrom
import com.example.simpleblev2.esp32ble.CUSTOM_SERVICE_UUID
import com.example.simpleblev2.esp32ble.Esp32Ble
import com.juul.kable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow


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


private val DISCONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5)

sealed class ConnectState {

    object Connecting : ConnectState()
    object Connected: ConnectState()
    object Disconnecting : ConnectState()
    object Disconnected : ConnectState()
}

val ConnectState.label: CharSequence
    get() = when (this) {
        ConnectState.Connecting -> "Connecting"
        is ConnectState.Connected -> "Connected"
        ConnectState.Disconnecting -> "Disconnecting"
        ConnectState.Disconnected -> "Disconnected"
    }


class MainViewModel : ViewModel() {
    private val scanner = Scanner {
        filters = listOf(
            Filter.Service(uuidFrom(CUSTOM_SERVICE_UUID))
        )
    }
    private val scanScope = viewModelScope.childScope()
    //private val found = hashMapOf<String, Advertisement>()

    private val _status = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)
    val status = _status.asStateFlow()

    //private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    //val advertisements = _advertisements.asStateFlow()


    private val connectionAttempt = AtomicInteger()

    private lateinit var peripheral: Peripheral
    private lateinit var esp32: Esp32Ble
    private lateinit var connectState: Flow<ConnectState>

    private val _state = MutableLiveData<String>()
    val state: LiveData<String>
        get() = _state

    private val _deviceList = MutableLiveData<MutableList<Device>>()
    val deviceList: LiveData<MutableList<Device>>
        get() = _deviceList

    private var deviceSelected = "Wähle Device"

    fun getDeviceList(): List<Device>? {
        return _deviceList.value
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
                _state.value = state.toString()
            }
            esp32.data.collect { data ->
                     Log.i(">>>>> data:", data.toString())
            }
        }
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

    fun connectDevice() {
        //viewModelScope.enableAutoReconnect()
        viewModelScope.connect()
    }

    fun disconnectDevice() {
        viewModelScope.disconnect()
    }

    private fun CoroutineScope.enableAutoReconnect() {
        peripheral.state
            .filter { it is State.Disconnected }
            .onEach {
                val timeMillis =
                    backoff(base = 500L, multiplier = 2f, retry = connectionAttempt.getAndIncrement())
                Log.i(">>>>", "Waiting $timeMillis ms to reconnect..." )
                delay(timeMillis)
                connect()
            }
            .launchIn(this)
    }

    private fun CoroutineScope.connect() {

        connectionAttempt.incrementAndGet()
        launch {
            Log.i(">>>>", "connect")
            try {
                peripheral.connect()
                //esp.enableGyro()
                //sensorTag.writeGyroPeriodProgress(periodProgress.get())
                connectionAttempt.set(0)
            } catch (e: ConnectionLostException) {
                Log.i(">>>>", "Connection attempt failed" )
            }
        }
    }

    private fun CoroutineScope.disconnect() {

        launch {
            Log.i(">>>>", "disconnect")
            try {
                peripheral.disconnect()
            } catch (e: ConnectionLostException) {
                Log.i(">>>>", "Disconnection attempt failed" )
            }
        }
    }


    // Extension Function, um Änderung in den Einträgen von Listen
    // dem Observer anzeigen zu können
    fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

}

/**
 * Exponential backoff using the following formula:
 *
 * ```
 * delay = base * multiplier ^ retry
 * ```
 *
 * For example (using `base = 100` and `multiplier = 2`):
 *
 * | retry | delay |
 * |-------|-------|
 * |   1   |   100 |
 * |   2   |   200 |
 * |   3   |   400 |
 * |   4   |   800 |
 * |   5   |  1600 |
 * |  ...  |   ... |
 *
 * Inspired by:
 * [Exponential Backoff And Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
 *
 * @return Backoff delay (in units matching [base] units, e.g. if [base] units are milliseconds then returned delay will be milliseconds).
 */
private fun backoff(
    base: Long,
    multiplier: Float,
    retry: Int,
): Long = (base * multiplier.pow(retry - 1)).toLong()