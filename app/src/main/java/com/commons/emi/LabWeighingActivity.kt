package com.commons.emi

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LabWeighingActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_weighing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        title = "Weighing screen"
    }
}