package com.example.simpleblev2

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.simpleblev2.databinding.FragmentEsp32controlBinding
import com.example.simpleblev2.model.ConnectState
import com.example.simpleblev2.model.MainViewModel
import com.juul.kable.State


class ESP32ControlFragment : Fragment() {

    private var _binding: FragmentEsp32controlBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentEsp32controlBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.tvSelectedDevice.text = viewModel.getDeviceSelected()

        // Mittels Observer über Änderungen des connect status informieren
        viewModel.connectState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ConnectState.CONNECTED -> {
                    binding.tvIsConnected.text = getString(R.string.connected)
                    binding.btnConnect.isEnabled = false
                    binding.btnDisconnect.isEnabled = true
                }
                ConnectState.NOT_CONNECTED -> {
                    binding.tvIsConnected.text = getString(R.string.not_connected)
                    binding.btnConnect.isEnabled = true
                    binding.btnDisconnect.isEnabled = false
                }
                ConnectState.NO_DEVICE -> {
                    binding.tvIsConnected.text = getString(R.string.no_selected_device)
                    binding.btnConnect.isEnabled = false
                    binding.btnDisconnect.isEnabled = false
                }
                ConnectState.DEVICE_SELECTED -> {
                    binding.tvIsConnected.text = getString(R.string.connecting)
                    binding.btnConnect.isEnabled = true
                    binding.btnDisconnect.isEnabled = false
                }
            }
        }

        // Observer auf neue Daten vom ESP32
        viewModel.esp32Data.observe(viewLifecycleOwner) { data ->
            binding.tvData.text = "${data.ledstatus}\n${data.potiArray}"
            // process data ....
        }

        binding.btnSelectDevice.setOnClickListener {
            findNavController().navigate(R.id.action_ESP32ControlFragment_to_manageDeviceFragment)
        }

        binding.btnConnect.setOnClickListener {
            viewModel.connect()
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }

        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.ledData.led = "H"
            else viewModel.ledData.led = "L"
            viewModel.sendLedData()
        }

        binding.switchBlinken.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.ledData.ledBlinken = true
            else viewModel.ledData.ledBlinken = false
            viewModel.sendLedData()
        }

        binding.switchDaten.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.startDataLoadJob()
                binding.tvData.visibility = View.VISIBLE
            } else {
                viewModel.cancelDataLoadJob()
                binding.tvData.visibility = View.INVISIBLE
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}