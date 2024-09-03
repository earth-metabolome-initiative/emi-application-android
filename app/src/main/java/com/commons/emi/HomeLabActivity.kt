package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button

@Suppress("DEPRECATION")
class HomeLabActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home_lab
    }

    private lateinit var preparingButton: Button
    private lateinit var weighingButton: Button
    private lateinit var extractionButton: Button
    private lateinit var aliquotingButton: Button

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Lab screen"

        PermissionsManager.enableBluetoothAndLocation(this)

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        preparingButton = findViewById(R.id.preparingButton)
        weighingButton = findViewById(R.id.weighingButton)
        extractionButton = findViewById(R.id.extractionButton)
        aliquotingButton = findViewById(R.id.aliquotingButton)

        // Set up button click listeners here
        preparingButton.setOnClickListener {
            val intent = Intent(this, LabPrepActivity::class.java)
            startActivity(intent)
        }

        weighingButton.setOnClickListener {
            //val intent = Intent(this, PrinterAcceptActivity::class.java)
            intent.putExtra("ACTIVITY", "LabWeighingActivity")
            startActivity(intent)
        }

        extractionButton.setOnClickListener {
            //val intent = Intent(this, PrinterAcceptActivity::class.java)
            intent.putExtra("ACTIVITY", "LabExtractionActivity")
            startActivity(intent)
        }

        aliquotingButton.setOnClickListener {
            //val intent = Intent(this, PrinterAcceptActivity::class.java)
            intent.putExtra("ACTIVITY", "LabAliquotingActivity")
            startActivity(intent)
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