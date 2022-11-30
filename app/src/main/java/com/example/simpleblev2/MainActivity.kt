package com.example.simpleblev2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.PermissionChecker
import com.example.simpleblev2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkBTPermission()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)


    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        val bluetooth = BluetoothAdapter.getDefaultAdapter()

        // ist BT auf dem Device verfügbar?
        if(bluetooth == null)
        {
            Toast.makeText(this, getString(R.string.bt_not_available), Toast.LENGTH_LONG).show()
            finish();
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, getString(R.string.ble_not_supported), Toast.LENGTH_LONG).show()
            finish()
        }
        // ist BT eingeschaltet?
        if (!bluetooth.isEnabled) {
            val turnBTOn = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(turnBTOn, 1)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun checkBTPermission() {
        var permissionCheck = PermissionChecker.checkSelfPermission(
            this,
            "Manifest.permission.ACCESS_FINE_LOCATION"
        )
        permissionCheck += PermissionChecker.checkSelfPermission(
            this,
            "Manifest.permission.ACCESS_COARSE_LOCATION"
        )
        permissionCheck += PermissionChecker.checkSelfPermission(
            this,
            "Manifest.permission.BLUETOOTH_CONNECT"
        )
        permissionCheck += PermissionChecker.checkSelfPermission(
            this,
            "Manifest.permission.BLUETOOTH_SCAN"
        )
        if (permissionCheck != PermissionChecker.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN), 1001)
        }
    }
}