package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button

@Suppress("DEPRECATION")
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

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        sampleButton = findViewById(R.id.sampleButton)
        rangeButton = findViewById(R.id.rangeButton)
        speciesButton = findViewById(R.id.speciesButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")

        // Set up button click listeners here
        sampleButton.setOnClickListener {
            val intent = Intent(this, SearchSampleActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        rangeButton.setOnClickListener {
            val intent = Intent(this, SearchRangeActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        speciesButton.setOnClickListener {
            val intent = Intent(this, SearchSpeciesActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
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