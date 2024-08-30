package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class HomeActivity : BaseActivity() {

    private lateinit var labButton: Button
    private lateinit var logisticButton: Button
    private lateinit var searchButton: Button
    private lateinit var signalButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home // Replace with your actual login layout file
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_home)

        title = "Home screen"

        // Request camera permission
        PermissionsManager.requestCameraPermission(this)

        labButton = findViewById(R.id.labButton)
        logisticButton = findViewById(R.id.logisticButton)
        searchButton = findViewById(R.id.searchButton)
        signalButton = findViewById(R.id.signalButton)


        // Set up button click listeners here
        labButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeLabActivity::class.java)
                startActivity(intent)
            } else {
                showToast("You need to accept camera permission.")
            }

        }

        logisticButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeLogisticActivity::class.java)
                startActivity(intent)
            } else {
                showToast("You need to accept camera permission.")
            }
        }

        searchButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeSearchActivity::class.java)
                startActivity(intent)
            } else {
                showToast("You need to accept camera permission.")
            }
        }

        signalButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeSignalingActivity::class.java)
                startActivity(intent)
        } else {
            showToast("You need to accept camera permission.")
        }
        }
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}