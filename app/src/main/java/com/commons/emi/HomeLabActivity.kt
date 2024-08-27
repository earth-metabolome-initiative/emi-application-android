package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class HomeLabActivity : AppCompatActivity() {

    private lateinit var preparingButton: Button
    private lateinit var weighingButton: Button
    private lateinit var extractionButton: Button
    private lateinit var aliquotingButton: Button

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_lab)

        title = "Lab screen"

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        preparingButton = findViewById(R.id.preparingButton)
        weighingButton = findViewById(R.id.weighingButton)
        extractionButton = findViewById(R.id.extractionButton)
        aliquotingButton = findViewById(R.id.aliquotingButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")
        val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

        // Set up button click listeners here
        preparingButton.setOnClickListener {
            val intent = Intent(this, HomeLabActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        weighingButton.setOnClickListener {
            if (isPrinterConnected == "yes") {
                //val intent = Intent(this, WeightingActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            } else {
                val activity = "WeightingActivity"
                //val intent = Intent(this, WarningActivity::class.java)
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
                //val intent = Intent(this, ExtractionActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            } else {
                val activity = "ExtractionActivity"
                //val intent = Intent(this, WarningActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("ACTIVITY", activity)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            }
        }

        aliquotingButton.setOnClickListener {
            if (isPrinterConnected == "yes") {
                //val intent = Intent(this, AliquotsActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            } else {
                val activity = "AliquotsActivity"
                //val intent = Intent(this, WarningActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("ACTIVITY", activity)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
            }
        }
    }

    // Connect the back arrow to the action to go back to home page
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}