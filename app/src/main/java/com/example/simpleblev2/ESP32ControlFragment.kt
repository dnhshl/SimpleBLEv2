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
import com.example.simpleblev2.model.MainViewModel
import com.juul.kable.State


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
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

        // Mittels Observer den Adapter über Änderungen in der Liste informieren
        viewModel.connectState.observe(viewLifecycleOwner) { state ->
            Log.i(">>>> state", state)
            when (state) {
                "Connected" -> {binding.tvIsConnected.text = getString(R.string.connected)}
                "Disconnected(null)" -> {binding.tvIsConnected.text = getString(R.string.not_connected)}
                else -> {binding.tvIsConnected.text = getString(R.string.connecting)}
            }
        }

        viewModel.esp32Data.observe(viewLifecycleOwner) { data ->
            binding.tvData.text = "${data.ledstatus}\n${data.potiArray}"
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