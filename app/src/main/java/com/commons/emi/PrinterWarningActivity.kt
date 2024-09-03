package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class PrinterWarningActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_printer_warning
    }

    override fun setupContentFrame() {
        TODO("Not yet implemented")
    }

    private lateinit var warningMessage: TextView
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Warning screen"

        // Initialize views
        warningMessage = findViewById(R.id.warningMessage)
        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)

        val activity = intent.getStringExtra("ACTIVITY")

        // Set up button click listeners here
        confirmButton.setOnClickListener {
            when (activity) {
                "LabWeighingActivity" -> {
                    val intent = Intent(this, LabWeighingActivity::class.java)
                    startActivity(intent)
                }

                "LabExtractionActivity" -> {
                    val intent = Intent(this, LabExtractionActivity::class.java)
                    startActivity(intent)
                }

                "LabAliquotingActivity" -> {
                    val intent = Intent(this, LabAliquotingActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        // Set up button click listeners here
        cancelButton.setOnClickListener {
            val intent = Intent(this, PrinterConnectActivity::class.java)
            startActivity(intent)
        }


    }
}