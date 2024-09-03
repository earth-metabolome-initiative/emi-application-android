package com.commons.emi

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

class LabPrepActivity : BaseActivity() {

    // Initiate the displayed objects
    private lateinit var textContainer: TextView
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

        title = "Preparation screen"

        // Initialize objects views
        textContainer = findViewById(R.id.textContainer)
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
            scanStatus.text = "Scan a sample"
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
                        scanButtonSample.visibility = View.INVISIBLE
                        textSample.visibility = View.INVISIBLE
                        emptyPlace.setTextColor(Color.RED)
                    } else if (places == -1) {
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text = "You are trying to scan a sample tube, please scan a valid container."
                        scanButtonSample.visibility = View.INVISIBLE
                        textSample.visibility = View.INVISIBLE
                        emptyPlace.setTextColor(Color.RED)
                    } else if (places == -3) {
                        textSample.visibility = View.VISIBLE
                        scanButtonSample.visibility = View.VISIBLE
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.setTextColor(Color.GRAY)
                        emptyPlace.text =
                            "This container is not determined as finite."
                    } else if (places == -4) {
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text = "Invalid container, please scan a valid one."
                        scanButtonSample.visibility = View.INVISIBLE
                        textSample.visibility = View.INVISIBLE
                        emptyPlace.setTextColor(Color.RED)
                    }else {
                        showToast("places: $places")
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text = "Unknown error, please restart the application."
                        scanButtonContainer.visibility = View.INVISIBLE
                        scanButtonSample.visibility = View.INVISIBLE
                        textSample.visibility = View.INVISIBLE
                        emptyPlace.setTextColor(Color.RED)
                    }
                } else if (isObjectScanActive) {
                    val containerId = scanButtonContainer.text.toString()
                    val sampleId = scanButtonSample.text.toString()
                    val containerModel = ContainerManager.getContainerModel(containerId)
                    val sampleModel = ContainerManager.getContainerModel(sampleId)
                    val isPairLegal = ContainerManager.checkContainerHierarchy(containerModel, sampleModel)
                    if (isPairLegal) {
                        val sampleKey = ContainerManager.getPrimaryKey(sampleId)
                        val containerKey = ContainerManager.getPrimaryKey(containerId)
                        withContext(Dispatchers.IO) {
                            sendDataToDirectus(sampleKey, containerKey)
                        }
                    } else {
                            emptyPlace.visibility = View.VISIBLE
                            emptyPlace.setTextColor(Color.RED)
                            emptyPlace.text = "Invalid pair. You are not allowed to put this child container in this parent container."
                        }
                }
            }
        }
    }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private suspend fun sendDataToDirectus(
        sampleKey: Int,
        containerKey: Int
    ) {
        // Perform the POST request to add the values on directus
        try {
            // Retrieve primary keys, token and URL
            val collectionUrl = DirectusTokenManager.getInstance() + "/items/Dried_Samples_Data"
            val accessToken = DirectusTokenManager.getAccessToken()

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
                showToast("Data correctly added to database")

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
                        "Data already added to database. If you want to move it to another container, please use the move stuff mode."
                }
            }
        } catch (e: IOException) {
            showToast("Error: $e")
            }
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}