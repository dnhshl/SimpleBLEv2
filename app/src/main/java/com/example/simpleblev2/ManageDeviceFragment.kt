package com.example.simpleblev2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.PermissionChecker
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.simpleblev2.databinding.FragmentMagageDeviceBinding
import com.example.simpleblev2.model.MainViewModel


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ManageDeviceFragment : Fragment() {

    private var _binding: FragmentMagageDeviceBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val bluetoothAdapter: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var scanner: BluetoothLeScanner
    private var isScanning = false

    private val discoveredDevices = arrayListOf<String>()

    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMagageDeviceBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanner = bluetoothAdapter.bluetoothLeScanner
        binding.btnSearchDevices.setOnClickListener {
            checkBTPermission()
            discoverDevices()
        }
        binding.listview.setOnItemClickListener { _, _, i, _ ->
            // Merke selected Device im ViewModel
            viewModel.setSelectedDevice(binding.listview.getItemAtPosition(i).toString())
            // Navigiere zurück zum ESP32ControlFragment
            findNavController().navigate(R.id.action_manageDeviceFragment_to_ESP32ControlFragment)
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        when (isScanning) {
            false -> {
                // Suche einschalten
                scanner.startScan(scanCallback)
                isScanning = true
                // Button Text anpassen
                binding.btnSearchDevices.text = getString(R.string.stop_search_devices)
            }
            true -> {
                // Suche ausschalten
                scanner.stopScan(scanCallback)
                isScanning = false
                // Button Text anpassen
                binding.btnSearchDevices.text = getString(R.string.start_search_devices)
            }
        }
    }


    private fun checkBTPermission() {
        var permissionCheck = PermissionChecker.checkSelfPermission(
            requireContext(),
            "Manifest.permission.ACCESS_FINE_LOCATION"
        )
        permissionCheck += PermissionChecker.checkSelfPermission(
            requireContext(),
            "Manifest.permission.ACCESS_COARSE_LOCATION"
        )
        permissionCheck += PermissionChecker.checkSelfPermission(
            requireContext(),
            "Manifest.permission.BLUETOOTH_CONNECT"
        )
        if (permissionCheck != PermissionChecker.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT), 1001)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroyView() {
        super.onDestroyView()
        if (isScanning) scanner.stopScan(scanCallback)
        _binding = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Wenn Devicename nicht ESP32 enthält, mache nichts
            if (result.device.name == null) return
            if (!result.device.name.contains("ESP32")) return

            val deviceInfo = """${result.device.name} ${result.device.address}""".trimIndent()
            Log.i(">>>>", "DeviceFound: $deviceInfo")

            // gefundenes Gerät der Liste hinzufügen, wenn es noch nicht aufgeführt ist
            if (deviceInfo !in discoveredDevices) discoveredDevices.add(deviceInfo)

            // aktualisierte Liste im Listview anzeigen
            val adapt = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1, discoveredDevices)
            binding.listview.adapter = adapt
        }
    }
}