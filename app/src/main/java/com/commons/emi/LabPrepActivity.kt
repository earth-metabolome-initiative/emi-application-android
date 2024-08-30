package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.camera.view.PreviewView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection

@Suppress("DEPRECATION")
class LabPrepActivity : BaseActivity() {

    // Initiate the displayed objects
    private lateinit var scanButtonContainer: Button
    private lateinit var emptyPlace: TextView
    private lateinit var textSample: TextView
    private lateinit var scanButtonSample: Button

    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var noneButton: Button
    private lateinit var scanStatus: TextView

    private var isContainerScanActive = false
    private var isObjectScanActive = false
    private var isQrScannerActive = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_prep
    }
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        title = "Preparation screen"

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        // Initialize objects views
        scanButtonContainer = findViewById(R.id.scanButtonContainer)
        emptyPlace = findViewById(R.id.emptyPlace)
        textSample = findViewById(R.id.textSample)
        scanButtonSample = findViewById(R.id.scanButtonSample)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        noneButton = findViewById(R.id.noneButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Set up button click listener for Container QR Scanner
        scanButtonContainer.setOnClickListener {
            isContainerScanActive = true
            isObjectScanActive = false
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan a container"
            flashlightButton.visibility = View.VISIBLE
            noneButton.visibility = View.VISIBLE
            scanButtonContainer.visibility = View.INVISIBLE
            textSample.visibility = View.INVISIBLE
            scanButtonSample.visibility = View.INVISIBLE
            ScanManager.initialize(this, previewView, flashlightButton, noneButton) {scannedContainer ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                noneButton.visibility = View.INVISIBLE
                scanButtonContainer.visibility = View.VISIBLE
                textSample.visibility = View.VISIBLE
                scanButtonSample.visibility = View.VISIBLE
                scanStatus.text = ""
                scanButtonContainer.text = scannedContainer
                manageScan()
            }
        }

        // Set up button click listener for falcon QR Scanner
        scanButtonSample.setOnClickListener {
            isContainerScanActive = false
            isObjectScanActive = true
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan a falcon"
            flashlightButton.visibility = View.VISIBLE
            scanButtonContainer.visibility = View.INVISIBLE
            textSample.visibility = View.INVISIBLE
            scanButtonSample.visibility = View.INVISIBLE
            ScanManager.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                scanButtonContainer.visibility = View.VISIBLE
                textSample.visibility = View.VISIBLE
                scanButtonSample.visibility = View.VISIBLE
                scanStatus.text = ""
                scanButtonSample.text = scannedSample
                manageScan()
            }
        }

    }

    // Function to store the scanned data.
    @SuppressLint("SetTextI18n")
    fun manageScan() {

        // Counts the spaces left in the rack
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (isContainerScanActive) {
                    if (scanButtonContainer.text.toString().matches(Regex("^container_\\d{6}$"))) {
                        // permits to calculate places in container
                        val places =
                            ContainerManager.checkContainer(scanButtonContainer.text.toString())
                        if (places > 0) {
                            textSample.visibility = View.VISIBLE
                            scanButtonSample.visibility = View.VISIBLE
                            emptyPlace.visibility = View.VISIBLE
                            emptyPlace.setTextColor(Color.GRAY)
                            emptyPlace.text =
                                "This container should still contain $places empty places"
                        } else if (places == 0) {
                            emptyPlace.visibility = View.VISIBLE
                            emptyPlace.text = "This container is full, please scan another one"
                            scanButtonContainer.text = "Value"
                            scanButtonSample.text = "Begin to scan samples"
                            emptyPlace.setTextColor(Color.RED)
                        } else if (places == -1) {
                            textSample.visibility = View.VISIBLE
                            scanButtonSample.visibility = View.VISIBLE
                            emptyPlace.visibility = View.VISIBLE
                            emptyPlace.setTextColor(Color.GRAY)
                            emptyPlace.text =
                                "This container is not determined as finite."
                        } else {
                            emptyPlace.visibility = View.VISIBLE
                            emptyPlace.text = "Unknown error, please restart the application."
                            scanButtonContainer.visibility = View.INVISIBLE
                            scanButtonSample.visibility = View.INVISIBLE
                            emptyPlace.setTextColor(Color.RED)
                        }
                    }
                } else if (isObjectScanActive) {
                    val containerId = scanButtonContainer.text.toString()
                    val sampleId = scanButtonSample.text.toString()
                    withContext(Dispatchers.IO) {
                        sendDataToDirectus(sampleId, containerId)
                    }
                }
            }
        }
    }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private suspend fun sendDataToDirectus(
        sampleId: String,
        containerId: String
    ) {
        // Perform the POST request to add the values on directus
        try {
            // Retrieve primary keys, token and URL
            val collectionUrl = DirectusTokenManager.getInstance() + "/items/Dried_Samples_Data"
            val accessToken = DirectusTokenManager.getAccessToken()
            showToast("sample id: $sampleId, container id: $containerId")
            val sampleKey = ContainerManager.getPrimaryKey(sampleId)
            val containerKey = ContainerManager.getPrimaryKey(containerId)

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

            val responseCode = response.code
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Display a Toast with the response message
                showToast("$sampleId correctly added to database")

                // Check if there is still enough place in the rack before initiating the QR code reader
                withContext(Dispatchers.Main) {
                    val places =
                        ContainerManager.checkContainer(scanButtonContainer.text.toString())
                    if (places > 0) {
                        // Automatically launch the QR scanning when last sample correctly added to the database
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text = "This rack should still contain $places empty places"
                        delay(500)
                        scanButtonSample.performClick()
                    } else if (places == -1) {
                        // Automatically launch the QR scanning when last sample correctly added to the database
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text = "This container is not determined as finite."
                        delay(500)
                        scanButtonSample.performClick()
                    } else if (places == 0) {
                        emptyPlace.text = "Container is full, scan another one to continue"
                        scanButtonContainer.text = "scan another container"
                        scanButtonSample.text = "Begin to scan samples"
                        textSample.visibility = View.INVISIBLE
                        scanButtonSample.visibility = View.INVISIBLE

                    } else {
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text = "Unknown error, please restart the application."
                        scanButtonContainer.visibility = View.INVISIBLE
                        scanButtonSample.visibility = View.INVISIBLE
                        emptyPlace.setTextColor(Color.RED)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    emptyPlace.visibility = View.VISIBLE
                    emptyPlace.text =
                        "sample $sampleId has already been added to database. If you want to move it to another container, please use the move stuff mode."
                }
            }
        } catch (e: IOException) {
            showToast("Error: $e")
            }
    }

    // Function to redirect user to connection page if connection is lost.
    private fun goToConnectionActivity(){
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    // Connect the back arrow to the action to go back to home page
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                return if (isQrScannerActive){
                    ScanManager.stopScanning()
                    isQrScannerActive = false
                    previewView.visibility = View.INVISIBLE
                    flashlightButton.visibility = View.INVISIBLE
                    scanButtonContainer.visibility = View.VISIBLE
                    noneButton.visibility = View.INVISIBLE
                    scanStatus.text = ""
                    if (isObjectScanActive){
                        textSample.visibility = View.VISIBLE
                        scanButtonSample.visibility = View.VISIBLE
                        noneButton.visibility = View.INVISIBLE
                    }
                    true
                } else {
                    onBackPressed()
                    true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}