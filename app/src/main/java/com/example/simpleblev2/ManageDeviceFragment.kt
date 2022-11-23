package com.example.simpleblev2

import android.Manifest
import android.os.Bundle
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

        // Adapter für den ListView
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1,   // Layout zur Darstellung der ListItems
            viewModel.getDeviceList()!!)   // Liste, die Dargestellt werden soll

        // Adapter an den ListView koppeln
        binding.listview.adapter = adapter

        // Mittels Observer den Adapter über Änderungen in der Liste informieren
        viewModel.deviceList.observe(viewLifecycleOwner) { adapter.notifyDataSetChanged() }

        binding.listview.setOnItemClickListener { _, _, i, _ ->
            // i ist der Index des geklickten Eintrags
            viewModel.setDeviceSelected(binding.listview.getItemAtPosition(i).toString())
            // Navigiere zurück zum ESP32ControlFragment
            findNavController().navigate(R.id.action_manageDeviceFragment_to_ESP32ControlFragment)
        }

        binding.btnSearchDevices.setOnClickListener {
            checkBTPermission()
            viewModel.startScan()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopScan()
        _binding = null
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
}