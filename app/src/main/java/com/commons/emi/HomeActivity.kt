package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var labButton: Button
    private lateinit var logisticButton: Button
    private lateinit var searchButton: Button
    private lateinit var signalButton: Button

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        title = "Home screen"

        labButton = findViewById(R.id.labButton)
        logisticButton = findViewById(R.id.logisticButton)
        searchButton = findViewById(R.id.searchButton)
        signalButton = findViewById(R.id.signalButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")

        // Set up button click listeners here
        labButton.setOnClickListener {
            val intent = Intent(this, HomeLabActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        logisticButton.setOnClickListener {
            val intent = Intent(this, HomeLogisticActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        searchButton.setOnClickListener {
            //val intent = Intent(this, ExtractionActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        signalButton.setOnClickListener {
            //val intent = Intent(this, AliquotsActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
    }
}