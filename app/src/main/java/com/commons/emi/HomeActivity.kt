package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var sampleButton: Button
    private lateinit var weightingButton: Button
    private lateinit var extractionButton: Button
    private lateinit var aliquotsButton: Button
    private lateinit var moveButton: Button
    private lateinit var signalingButton: Button
    private lateinit var findButton: Button

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        title = "Home screen"

        sampleButton = findViewById(R.id.sampleButton)
        weightingButton = findViewById(R.id.weightingButton)
        extractionButton = findViewById(R.id.extractionButton)
        aliquotsButton = findViewById(R.id.aliquotsButton)
        moveButton = findViewById(R.id.moveButton)
        signalingButton = findViewById(R.id.signalingButton)
        findButton = findViewById(R.id.findButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")
        val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

        // Set up button click listeners here
        sampleButton.setOnClickListener {
            val intent = Intent(this, FalconActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        weightingButton.setOnClickListener {
            if (isPrinterConnected == "yes") {
                val intent = Intent(this, WeightingActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            } else {
                val activity = "WeightingActivity"
                val intent = Intent(this, WarningActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("ACTIVITY", activity)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            }
        }

        extractionButton.setOnClickListener {
            if (isPrinterConnected == "yes") {
                val intent = Intent(this, ExtractionActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            } else {
                val activity = "ExtractionActivity"
                val intent = Intent(this, WarningActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("ACTIVITY", activity)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            }
        }

        aliquotsButton.setOnClickListener {
            if (isPrinterConnected == "yes") {
                val intent = Intent(this, AliquotsActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            } else {
                val activity = "AliquotsActivity"
                val intent = Intent(this, WarningActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("ACTIVITY", activity)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            }
        }

        moveButton.setOnClickListener {
            val intent = Intent(this, MoveActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        signalingButton.setOnClickListener {
            val intent = Intent(this, SignalingActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findButton.setOnClickListener {
            val intent = Intent(this, FindActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
    }
}