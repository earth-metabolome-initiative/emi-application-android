package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

abstract class HomeActivity : BaseActivity() {

    private lateinit var labButton: Button
    private lateinit var logisticButton: Button
    private lateinit var searchButton: Button
    private lateinit var signalButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home
    }

    override fun setupContentFrame() {
        layoutInflater.inflate(R.layout.activity_home, findViewById(R.id.activity_content), true)
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the link with the corresponding xml
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
                showToast()
            }

        }

        logisticButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeLogisticActivity::class.java)
                startActivity(intent)
            } else {
                showToast()
            }
        }

        searchButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeSearchActivity::class.java)
                startActivity(intent)
            } else {
                showToast()
            }
        }

        signalButton.setOnClickListener {
            val cameraPermission = PermissionsManager.checkCameraPermission(this)
            if (cameraPermission) {
                val intent = Intent(this, HomeSignalingActivity::class.java)
                startActivity(intent)
        } else {
            showToast()
        }
        }
    }

    // Function to easily display toasts.
    private fun showToast() {
        runOnUiThread { Toast.makeText(this, "You need to accept camera permission.", Toast.LENGTH_LONG).show() }
    }
}