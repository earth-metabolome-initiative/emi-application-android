package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class HomeSearchActivity : BaseActivity() {

    private lateinit var sampleButton: Button
    private lateinit var rangeButton: Button
    private lateinit var speciesButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home_search
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Search screen"

        sampleButton = findViewById(R.id.sampleButton)
        rangeButton = findViewById(R.id.rangeButton)
        speciesButton = findViewById(R.id.speciesButton)

        // Set up button click listeners here
        sampleButton.setOnClickListener {
            val intent = Intent(this, SearchSampleActivity::class.java)
            startActivity(intent)
        }

        rangeButton.setOnClickListener {
            val intent = Intent(this, SearchRangeActivity::class.java)
            startActivity(intent)
        }

        speciesButton.setOnClickListener {
            val intent = Intent(this, SearchSpeciesActivity::class.java)
            startActivity(intent)
        }
    }
}