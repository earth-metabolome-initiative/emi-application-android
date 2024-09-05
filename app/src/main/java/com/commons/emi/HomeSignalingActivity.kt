package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button

class HomeSignalingActivity : BaseActivity() {

    private lateinit var simpleButton: Button
    private lateinit var recursiveButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home_signaling
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Signaling screen"

        simpleButton = findViewById(R.id.simpleButton)
        recursiveButton = findViewById(R.id.recursiveButton)

        // Set up button click listeners here
        simpleButton.setOnClickListener {
            val intent = Intent(this, SignalingSimpleActivity::class.java)
            startActivity(intent)
        }

        recursiveButton.setOnClickListener {
            val intent = Intent(this, SignalingRecursiveActivity::class.java)
            startActivity(intent)
        }
    }
}