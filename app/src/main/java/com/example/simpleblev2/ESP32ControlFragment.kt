package com.example.simpleblev2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.simpleblev2.databinding.FragmentEsp32controlBinding
import com.example.simpleblev2.model.MainViewModel


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

        binding.btnSelectDevice.setOnClickListener {
            findNavController().navigate(R.id.action_ESP32ControlFragment_to_manageDeviceFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}