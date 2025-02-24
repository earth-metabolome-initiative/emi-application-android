package com.commons.emi

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class HomeLogisticActivity : BaseActivity() {

    private lateinit var attributeButton: Button
    private lateinit var moveButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_home_logistic
    }

    // Function that is launched when class is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Logistic"

        // Define the breadcrumb path for Home
        val breadcrumbs = listOf(
            Pair("Login", LoginActivity::class.java),
            Pair("Home", HomeActivity::class.java),
            Pair("Logistic", null)
        )

        // Set breadcrumbs in com.bruelhart.coulage.ch.brulhart.farmapp.BaseActivity
        setBreadcrumbs(breadcrumbs)

        attributeButton = findViewById(R.id.attributeButton)
        moveButton = findViewById(R.id.moveButton)

        // Set up button click listeners here
        attributeButton.setOnClickListener {
            val intent = Intent(this, LogisticAttributingActivity::class.java)
            startActivity(intent)
        }

        moveButton.setOnClickListener {
            val intent = Intent(this, LogisticMovingActivity::class.java)
            startActivity(intent)
        }
    }
}