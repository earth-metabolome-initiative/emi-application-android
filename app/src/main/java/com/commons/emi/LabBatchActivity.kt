package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class LabBatchActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_batch
    }

    private lateinit var driedBatchButton: Button
    private lateinit var extractionBatchButton: Button
    private lateinit var aliquotBatchButton: Button

    // Function that is launched when class is called.
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Batch screen"

        driedBatchButton = findViewById(R.id.driedBatchButton)
        extractionBatchButton = findViewById(R.id.extractionBatchButton)
        aliquotBatchButton = findViewById(R.id.aliquotBatchButton)

        // Set up button click listeners here
        driedBatchButton.setOnClickListener {
            val intent = Intent(this, CreateBatchesActivity::class.java)
            intent.putExtra("batch", "samples")
            startActivity(intent)
        }

        // Set up button click listeners here
        extractionBatchButton.setOnClickListener {
            val intent = Intent(this, CreateBatchesActivity::class.java)
            intent.putExtra("batch", "extraction")
            startActivity(intent)
        }

        // Set up button click listeners here
        aliquotBatchButton.setOnClickListener {
            val intent = Intent(this, CreateBatchesActivity::class.java)
            intent.putExtra("batch", "aliquots")
            startActivity(intent)
        }
    }
}