package com.commons.emi

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bradysdk.api.printerconnection.PrinterProperties
import com.bradysdk.api.printerdiscovery.DiscoveredPrinterInformation

class SignalingSimpleActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_signaling_simple
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "Simple signaling screen"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}