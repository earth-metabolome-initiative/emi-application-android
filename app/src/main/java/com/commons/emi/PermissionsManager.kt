@file:Suppress("DEPRECATION")

package com.commons.emi

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsManager {

    private const val REQUEST_CODE_BLUETOOTH = 1001
    private const val REQUEST_CODE_CAMERA = 1002
    private const val REQUEST_CODE_ENABLE_BLUETOOTH: Int = 1
    private const val REQUEST_CODE_ENABLE_LOCATION: Int = 2

    // Function to check and request Bluetooth permission
    fun requestBluetoothPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_BLUETOOTH
                )
            }
        }
    }

    // Function to check and request Camera permission
    fun requestCameraPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA
            )
        }
    }

    fun checkCameraPermission(activity: Activity): Boolean {
        return (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
    }

    fun checkBluetoothPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED)
        } else {
            return true
        }
    }

    // Function to check if Bluetooth is enabled and request to enable it
    fun enableBluetoothAndLocation(activity: Activity) {
        val bluetoothSniffer = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothEnabled = bluetoothSniffer?.isEnabled ?: false

        val locationSniffer = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationSniffer.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isBluetoothEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            activity.startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH)
        }

        if (!isLocationEnabled) {
            val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivityForResult(locationIntent, REQUEST_CODE_ENABLE_LOCATION)
        }
    }

}