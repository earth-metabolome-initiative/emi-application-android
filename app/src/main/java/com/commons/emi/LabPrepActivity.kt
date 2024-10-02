package com.commons.emi

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection

class LabPrepActivity : BaseActivity() {

    // Initiate the displayed objects
    private lateinit var containerLayout: View
    private lateinit var textContainer: TextView
    private lateinit var scanButtonContainer: Button
    private lateinit var emptyPlace: TextView

    private lateinit var sampleLayout: View
    private lateinit var textSample: TextView
    private lateinit var scanButtonSample: Button

    private lateinit var scanLayout: LinearLayout
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var noneButton: Button
    private lateinit var scanStatus: TextView

    // Define trackers
    private var isQrScannerActive = false
    private var isContainerScanActive = false
    private var isContainerValid = false
    private var isObjectScanActive = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_prep
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Preparation screen"

        // Initialize objects views
        containerLayout = findViewById(R.id.containerLayout)
        textContainer = findViewById(R.id.textContainer)
        scanButtonContainer = findViewById(R.id.scanButtonContainer)
        emptyPlace = findViewById(R.id.emptyPlace)

        sampleLayout = findViewById(R.id.sampleLayout)
        textSample = findViewById(R.id.textSample)
        scanButtonSample = findViewById(R.id.scanButtonSample)

        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        closeButton = findViewById(R.id.closeButton)
        noneButton = findViewById(R.id.noneButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Set up button click listener for Container QR Scanner
        scanButtonContainer.setOnClickListener {
            isContainerScanActive = true
            isObjectScanActive = false
            isQrScannerActive = true
            visibilityManager()
            noneButton.visibility = View.VISIBLE
            scanStatus.text = "Scan a container"
            ScanManager.initialize(this, previewView, flashlightButton, noneButton) {scannedContainer ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                noneButton.visibility = View.GONE
                scanButtonContainer.textSize = 25f
                scanButtonContainer.text = scannedContainer
                manageScan()
            }
        }

        // Set up button click listener for sample's QR Scanner
        scanButtonSample.setOnClickListener {
            isContainerScanActive = false
            isObjectScanActive = true
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan a sample"
            ScanManager.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonSample.textSize = 25f
                scanButtonSample.text = scannedSample
                manageScan()
            }
        }

        closeButton.setOnClickListener {
            ScanManager.stopScanning()
            isQrScannerActive = false
            visibilityManager()
        }

    }

    private fun manageScan() {
        if (isContainerScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val places = DatabaseManager.checkContainer(scanButtonContainer.text.toString())
                withContext(Dispatchers.Main) {
                    handleContainerScan(places)
                }
            }
        } else if (isObjectScanActive) {
            val containerId = scanButtonContainer.text.toString()
            val sampleId = scanButtonSample.text.toString()
            handleObjectScan(containerId, sampleId)
        }
    }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private suspend fun sendDataToDirectus(
        sampleKey: Int,
        containerKey: Int,
        sampleId: String
    ) {
        // Perform the POST request to add the values on directus
        try {
            // Retrieve primary keys, token and URL
            val collectionUrl = DatabaseManager.getInstance() + "/items/Dried_Samples_Data"
            val accessToken = DatabaseManager.getAccessToken()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("sample_container", sampleKey)
                put("parent_container", containerKey)
                put("status", "present")
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(collectionUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            withContext(Dispatchers.Main) {

                val responseCode = response.code
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Display a Toast with the response message
                    showToast("$sampleId correctly added to database")

                    // Check if there is still enough place in the container before initiating the QR code reader
                    val places = DatabaseManager.checkContainer(scanButtonContainer.text.toString())
                    handleContainerScan(places)
                    if (places > 0) {
                        scanButtonSample.performClick()
                    }
                } else {
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text =
                            "$sampleId already added to database.\nIf you want to move it, please use the move stuff mode."
                    }
            }
        } catch (e: IOException) {
            showToast("Error: $e")
            }
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForValidContainer(places: Int) {
        isContainerValid = true
        visibilityManager()
        emptyPlace.setTextColor(Color.GRAY)
        emptyPlace.text = "This container should still contain $places empty places"
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForFullContainer() {
        isContainerValid = false
        visibilityManager()
        emptyPlace.text = "This container is full, please scan another one"
        scanButtonContainer.text = "Value"
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForSampleTubeError() {
        isContainerValid = false
        visibilityManager()
        emptyPlace.text = "You are trying to scan a sample tube, please scan a valid container."
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForIndeterminateContainer() {
        isContainerValid = true
        visibilityManager()
        emptyPlace.setTextColor(Color.GRAY)
        emptyPlace.text = "This container is not determined as finite."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForInvalidContainer() {
        isContainerValid = false
        visibilityManager()
        emptyPlace.text = "Invalid container, please scan a valid one."
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForUnknownError() {
        isContainerValid = false
        visibilityManager()
        emptyPlace.text = "Unknown error, please restart the application."
        emptyPlace.setTextColor(Color.RED)
    }

    private fun handleContainerScan(places: Int) {
        when (places) {
            in 1..Int.MAX_VALUE -> updateUIForValidContainer(places)
            0 -> updateUIForFullContainer()
            -1 -> updateUIForSampleTubeError()
            -3 -> updateUIForIndeterminateContainer()
            -4 -> updateUIForInvalidContainer()
            else -> updateUIForUnknownError()
        }
    }

    @SuppressLint("SetTextI18n")
    fun handleObjectScan(containerId: String, sampleId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val containerModel = DatabaseManager.getContainerModel(containerId)
            val sampleModel = DatabaseManager.getContainerModel(sampleId)
            val isPairLegal = DatabaseManager.checkContainerHierarchy(containerModel, sampleModel)
            if (isPairLegal) {
                val sampleKey = DatabaseManager.getPrimaryKey(sampleId)
                val containerKey = DatabaseManager.getPrimaryKey(containerId)
                withContext(Dispatchers.IO) {
                    sendDataToDirectus(sampleKey, containerKey, sampleId)
                }
            } else {
                withContext(Dispatchers.Main) {
                    emptyPlace.setTextColor(Color.RED)
                    emptyPlace.text =
                        "Invalid pair. You are not allowed to put this child container in this parent container."
                }
            }
        }
    }

    private fun visibilityManager () {

        if (isQrScannerActive) {
            scanLayout.visibility = View.VISIBLE
        } else {
            scanLayout.visibility = View.GONE
        }
        if (isContainerValid) {
            sampleLayout.visibility = View.VISIBLE
        } else {
            sampleLayout.visibility = View.GONE
        }
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}