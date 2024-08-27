package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class HomeLogisticActivity : AppCompatActivity() {

    private lateinit var attributeButton: Button
    private lateinit var moveButton: Button

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_logistic)

        title = "Lab screen"

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        attributeButton = findViewById(R.id.attributeButton)
        moveButton = findViewById(R.id.moveButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")
        val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

        // Set up button click listeners here
        attributeButton.setOnClickListener {
            //val intent = Intent(this, HomeLabActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        moveButton.setOnClickListener {
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