package com.commons.emi

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignalingRecursiveActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_signaling_recursive
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Report recursively"

        // Define the breadcrumb path for Home
        val breadcrumbs = listOf(
            Pair("Login", LoginActivity::class.java),
            Pair("Home", HomeActivity::class.java),
            Pair("Reporting", HomeSignalingActivity::class.java),
            Pair("Report recursively", null)
        )

        setBreadcrumbs(breadcrumbs)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}