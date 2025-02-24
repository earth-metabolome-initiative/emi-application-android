package com.commons.emi

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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

class LogisticMovingActivity : BaseActivity() {

    private lateinit var parentContainerLayout: View
    private lateinit var textParentContainer: TextView
    private lateinit var scanButtonParentContainer: Button
    private lateinit var emptyPlace: TextView

    private lateinit var childContainerLayout: View
    private lateinit var textChildContainer: TextView
    private lateinit var scanButtonChildContainer: Button

    private lateinit var scanLayout: LinearLayout
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var scanStatus: TextView

    // Define variables
    private var parentContainerModelId: Int = 0
    private var parentContainerId: Int = 0

    private var childContainerModelId: Int = 0
    private var childContainerId: Int = 0

    // Define trackers
    private var isQrScannerActive = false
    private var isParentScanActive = false
    private var isParentContainerValid = false
    private var isChildScanActive = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_logistic_moving
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Move containers"

        // Define the breadcrumb path for Home
        val breadcrumbs = listOf(
            Pair("Login", LoginActivity::class.java),
            Pair("Home", HomeActivity::class.java),
            Pair("Logistic", HomeLogisticActivity::class.java),
            Pair("Move containers", null)
        )

        // Set breadcrumbs in com.bruelhart.coulage.ch.brulhart.farmapp.BaseActivity
        setBreadcrumbs(breadcrumbs)

        parentContainerLayout = findViewById(R.id.parentContainerLayout)
        textParentContainer = findViewById(R.id.textParentContainer)
        scanButtonParentContainer = findViewById(R.id.scanButtonParentContainer)
        emptyPlace = findViewById(R.id.emptyPlace)

        childContainerLayout = findViewById(R.id.childContainerLayout)
        textChildContainer = findViewById(R.id.textChildContainer)
        scanButtonChildContainer = findViewById(R.id.scanButtonChildContainer)

        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        closeButton = findViewById(R.id.closeButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Set up button click listener for Container QR Scanner
        scanButtonParentContainer.setOnClickListener {
            isParentScanActive = true
            isChildScanActive = false
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan parent container"
            ScanManager.initialize(this, previewView, flashlightButton) {scannedContainer ->
                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonParentContainer.textSize = 25f
                scanButtonParentContainer.text = scannedContainer
                manageScan()
            }
        }

        // Set up button click listener for Container QR Scanner
        scanButtonChildContainer.setOnClickListener {
            isParentScanActive = false
            isChildScanActive = true
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan child container"
            ScanManager.initialize(this, previewView, flashlightButton) {scannedContainer ->
                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonChildContainer.textSize = 25f
                scanButtonChildContainer.text = scannedContainer
                manageScan()
            }
        }

        closeButton.setOnClickListener {
            ScanManager.stopScanning()
            isQrScannerActive = false
            visibilityManager()
        }

    }

    @SuppressLint("SetTextI18n")
    private fun manageScan() {
        if (isParentScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                parentContainerId = DatabaseManager.getContainerIdIfValid(scanButtonParentContainer.text.toString(), false)
                val places = DatabaseManager.checkContainerLoad(parentContainerId)
                parentContainerModelId = DatabaseManager.getContainerModelId(parentContainerId)
                Log.d("parent container models", "places: $places, parentContainerId: $parentContainerId, parentModelContainerId: $parentContainerModelId")
                withContext(Dispatchers.Main) {
                    handleParentContainerScan(places)
                }
            }
        } else if (isChildScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val childContainer = scanButtonChildContainer.text.toString()
                val parentContainer = scanButtonParentContainer.text.toString()
                childContainerId = DatabaseManager.getContainerId(childContainer)
                childContainerModelId = DatabaseManager.getContainerModelId(childContainerId)
                Log.d("child container models", "places: childContainerId: $childContainerId, childContainerModelId: $childContainerModelId")
                withContext(Dispatchers.Main) {
                    handleChildContainerScan(childContainer, parentContainer)
                }
            }
        }
    }

    private fun handleParentContainerScan(places: Int) {
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
    fun updateUIForValidContainer(places: Int) {
        isParentContainerValid = true
        visibilityManager()
        emptyPlace.setTextColor(Color.GRAY)
        emptyPlace.text = "This container should still contain $places empty places"
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForFullContainer() {
        isParentContainerValid = false
        visibilityManager()
        emptyPlace.text = "This container is full, please scan another one"
        scanButtonParentContainer.text = "Value"
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForSampleTubeError() {
        isParentContainerValid = false
        visibilityManager()
        emptyPlace.text = "You are trying to scan a sample tube, please scan a valid container."
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForIndeterminateContainer() {
        isParentContainerValid = true
        visibilityManager()
        emptyPlace.setTextColor(Color.GRAY)
        emptyPlace.text = "This container is not determined as finite."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForInvalidContainer() {
        isParentContainerValid = false
        visibilityManager()
        emptyPlace.text = "Invalid container, please scan a valid one."
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForUnknownError() {
        isParentContainerValid = false
        visibilityManager()
        emptyPlace.text = "Unknown error, please restart the application."
        emptyPlace.setTextColor(Color.RED)
    }

    @SuppressLint("SetTextI18n")
    fun handleChildContainerScan(childContainer: String, parentContainer: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val isPairLegal = DatabaseManager.checkContainerHierarchy(parentContainerModelId, childContainerModelId)
            if (isPairLegal) {
                withContext(Dispatchers.IO) {
                    sendDataToDirectus(parentContainer, childContainer, parentContainerId, childContainerId)
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

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private suspend fun sendDataToDirectus(
        parentContainer: String,
        childContainer: String,
        parentContainerId: Int,
        childContainerId: Int
    ) {
        // Perform the PATCH request to add the values on directus
        try {
            // Retrieve primary keys, token and URL
            val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers/$childContainerId"
            val accessToken = DatabaseManager.getAccessToken()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("parent_container", parentContainerId)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(collectionUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .patch(requestBody)
                .build()

            val response = client.newCall(request).execute()

            val responseCode = response.code
            if (responseCode == HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    showToast("$childContainer correctly added to $parentContainer")

                    // Check if there is still enough place in the container before initiating the QR code reader
                    val places = DatabaseManager.checkContainerLoad(parentContainerId)
                    handleParentContainerScan(places)
                    if (places > 0) {
                        scanButtonChildContainer.performClick()
                    }
                }
            }
        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    private fun visibilityManager () {
        if (isParentContainerValid) {
            childContainerLayout.visibility = View.VISIBLE
        } else {
            childContainerLayout.visibility = View.GONE
        }

        if (isQrScannerActive) {
            scanLayout.visibility = View.VISIBLE
        } else {
            scanLayout.visibility = View.GONE
        }
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}