package com.example.simpleblev2.esp32ble

import android.util.Log
import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val CUSTOM_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB"

private val CustomConfigCharacteristic = characteristicOf(
    service = CUSTOM_SERVICE_UUID,
    characteristic = "0000FFE1-0000-1000-8000-00805F9B34FB",
)


class Esp32Ble(
    private val peripheral: Peripheral
) : Peripheral by peripheral {

    val data: Flow<String> = peripheral.observe(CustomConfigCharacteristic)
        .map { data ->
            data.toString()
        }

    suspend fun writeLedData(jsonstring: String) {
        val data = byteArrayOf(jsonstring.toByte())

        peripheral.write(CustomConfigCharacteristic, data, WriteType.WithoutResponse)
        Log.i(">>>>", "write LED Data")
    }

}