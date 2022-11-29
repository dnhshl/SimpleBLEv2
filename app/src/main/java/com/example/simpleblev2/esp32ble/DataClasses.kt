package com.example.simpleblev2.esp32ble

import org.json.JSONArray

data class LedData(var led: String = "L", var ledBlinken: Boolean = false)
data class Esp32Data(val ledstatus: String = "", val potiArray: JSONArray = JSONArray())

