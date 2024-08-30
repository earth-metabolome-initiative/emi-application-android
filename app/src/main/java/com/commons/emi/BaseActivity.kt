package com.commons.emi

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var connectionStatusIcon: ImageView
    private lateinit var printerStatusIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        connectionStatusIcon = findViewById(R.id.connection_status_icon)
        printerStatusIcon = findViewById(R.id.printer_status_icon)

        // Inflate the child activity's layout into the container
        val inflater = layoutInflater
        val container = findViewById<FrameLayout>(R.id.activity_content)
        inflater.inflate(getLayoutResourceId(), container, true)

        // Observe connection status changes
        DirectusTokenManager.isConnected.observe(this) { isConnected ->
            updateConnectionStatus(isConnected)
        }

        // Observe printer status changes
        PrinterManager.isConnected.observe(this) { isConnected ->
            updatePrinterStatus(isConnected)
        }
    }

    @LayoutRes
    protected abstract fun getLayoutResourceId(): Int

    private fun updateConnectionStatus(isConnected: Boolean) {
        connectionStatusIcon.setImageResource(
            if (isConnected) R.drawable.ic_db_status_green
            else R.drawable.ic_db_status_red
        )
        connectionStatusIcon.visibility = View.VISIBLE
    }

    private fun updatePrinterStatus(isConnected: Boolean) {
        printerStatusIcon.setImageResource(
            if (isConnected) R.drawable.ic_green_printer
            else R.drawable.ic_red_printer
        )
        printerStatusIcon.visibility = View.VISIBLE
    }
}
