package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button

@Suppress("DEPRECATION")
class HomeSignalingActivity : BaseActivity() {

    private lateinit var simpleButton: Button
    private lateinit var recursiveButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home_signaling// Replace with your actual login layout file
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_signaling)

        title = "Signaling screen"

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        simpleButton = findViewById(R.id.simpleButton)
        recursiveButton = findViewById(R.id.recursiveButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")

        // Set up button click listeners here
        simpleButton.setOnClickListener {
            val intent = Intent(this, SignalingSimpleActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        recursiveButton.setOnClickListener {
            val intent = Intent(this, SignalingRecursiveActivity::class.java)
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