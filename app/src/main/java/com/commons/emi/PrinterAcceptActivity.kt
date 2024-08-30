package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class PrinterAcceptActivity : BaseActivity() {

    private lateinit var connectButton: Button
    private lateinit var ignoreButton: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_printer_accept
    }
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Printer connection screen"

        // Initialize views
        connectButton = findViewById(R.id.connectButton)
        ignoreButton = findViewById(R.id.ignoreButton)

        val activity = intent.getStringExtra("ACTIVITY")

        // Set up button click listeners here
        connectButton.setOnClickListener {
            PermissionsManager.requestBluetoothPermission(this)
            val intent = Intent(this,PrinterConnectActivity::class.java)
            intent.putExtra("ACTIVITY", activity)
            finish()
        }
        ignoreButton.setOnClickListener {
            val intent = Intent(this, PrinterWarningActivity::class.java)
            intent.putExtra("ACTIVITY", activity)
            startActivity(intent)
            finish()
        }
    }
}