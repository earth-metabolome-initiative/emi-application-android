package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button

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
            val intent = Intent(this, LabWeighingActivity::class.java)
            startActivity(intent)
        }

        extractionButton.setOnClickListener {
            val intent = Intent(this, LabExtractionActivity::class.java)
            startActivity(intent)
        }

        aliquotingButton.setOnClickListener {
            val intent = Intent(this, LabAliquotingActivity::class.java)
            startActivity(intent)
        }
    }
}