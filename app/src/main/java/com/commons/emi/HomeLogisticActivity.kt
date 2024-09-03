package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button

@Suppress("DEPRECATION")
class HomeLogisticActivity : BaseActivity() {

    private lateinit var attributeButton: Button
    private lateinit var moveButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home_logistic
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Logistic screen"

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        attributeButton = findViewById(R.id.attributeButton)
        moveButton = findViewById(R.id.moveButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")

        // Set up button click listeners here
        attributeButton.setOnClickListener {
            val intent = Intent(this, LogisticAttributingActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        moveButton.setOnClickListener {
            val intent = Intent(this, LogisticMovingActivity::class.java)
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