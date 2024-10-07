package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
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

    private lateinit var sampleContainerLayout: View
    private lateinit var textSampleContainer: TextView
    private lateinit var textNewSampleContainer: TextView
    private lateinit var containerModelSpinner: Spinner

    private lateinit var sampleLayout: View
    private lateinit var textBatch: TextView
    private lateinit var batchSpinner: Spinner
    private lateinit var batchDescriptionText: TextView
    private lateinit var textSample: TextView
    private lateinit var scanButtonSample: Button

    private lateinit var scanLayout: LinearLayout
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var noneButton: Button
    private lateinit var scanStatus: TextView

    // Define variables
    private var choices: List<String> = mutableListOf("Choose an option")
    private var sampleContainerModelId: Int = 0
    private var sampleContainerId: Int = 0
    private var containerModelId: Int = 0
    private var containerId: Int = 0
    private var batchId: Int = 0
    private var batchDescription: String = ""

    // Define trackers
    private var isQrScannerActive = false
    private var isContainerScanActive = false
    private var isContainerValid = false
    private var isContainerModelFilled = false
    private var isBatchSelected = false
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

        sampleContainerLayout = findViewById(R.id.sampleContainerLayout)
        textSampleContainer = findViewById(R.id.textSampleContainer)
        textNewSampleContainer = findViewById(R.id.textNewSampleContainer)
        containerModelSpinner = findViewById(R.id.containerModelSpinner)

        sampleLayout = findViewById(R.id.sampleLayout)
        textBatch = findViewById(R.id.textBatch)
        batchSpinner = findViewById(R.id.batchSpinner)
        batchDescriptionText = findViewById(R.id.batchDescriptionText)
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

        // Make the link clickable for information text to create a new container model.
        val linkTextView: TextView = textNewSampleContainer
        val spannableString = SpannableString(linkTextView.text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "${DatabaseManager.getInstance()}/admin/content/Container_Models/+"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }
        spannableString.setSpan(clickableSpan, 0, spannableString.length, spannableString.length)
        linkTextView.text = spannableString
        linkTextView.movementMethod = LinkMovementMethod.getInstance()

        // Fetch container models and populate the container models spinner.
        fetchValuesAndPopulateContainerModelsSpinner()

        fetchValuesAndPopulateBatchSpinner()

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

    // Function to obtain extraction methods from directus and to populate the spinner.
    private fun fetchValuesAndPopulateContainerModelsSpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val collectionUrl =
                    "${DatabaseManager.getInstance()}/items/Container_Models"
                val client = OkHttpClient()
                val request = Request.Builder()
                    .addHeader("Accept", "application/json")
                    .url(collectionUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == HttpURLConnection.HTTP_OK) {
                    val responseBody = response.body?.string()

                    val jsonObject = responseBody?.let { JSONObject(it) }
                    val dataArray = jsonObject?.getJSONArray("data")

                    val values = ArrayList<String>()
                    val ids = HashMap<String, Int>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose a sample container model")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val containerTypeId = item.optInt("container_type")
                            val containerType = DatabaseManager.getContainerType(containerTypeId)
                            val volume = item.optDouble("volume")
                            val volumeUnitId = item.optInt("volume_unit")
                            val volumeUnit = DatabaseManager.getUnit(volumeUnitId)
                            val brandId = item.optInt("brand")
                            val brand = DatabaseManager.getBrand(brandId)
                            val value = "$containerType $volume $volumeUnit $brand"
                            val id = item.optInt("id")
                            if (volume > 0 && volumeUnit != "pcs") {
                                values.add(value)
                                ids[value] = id
                            }
                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@LabPrepActivity,
                            R.layout.spinner_list,
                            values
                        )
                        adapter.setDropDownViewResource(R.layout.spinner_list)
                        containerModelSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        containerModelSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                @SuppressLint("SetTextI18n")
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                                        val selectedValue = values[position]
                                        sampleContainerModelId = ids[selectedValue].toString().toInt()
                                        isContainerModelFilled = true
                                        visibilityManager()
                                    } else {
                                        isContainerModelFilled = false
                                        visibilityManager()
                                    }
                                }

                                @SuppressLint("SetTextI18n")
                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    isContainerModelFilled = false
                                    visibilityManager()
                                }
                            }
                    }
                } else {
                    showToast("Connection error")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("$e")
            }
        }
    }

    // Function to obtain extraction methods from directus and to populate the spinner.
    private fun fetchValuesAndPopulateBatchSpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val collectionUrl =
                    "${DatabaseManager.getInstance()}/items/Batches"
                val client = OkHttpClient()
                val request = Request.Builder()
                    .addHeader("Accept", "application/json")
                    .url(collectionUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == HttpURLConnection.HTTP_OK) {
                    val responseBody = response.body?.string()

                    val jsonObject = responseBody?.let { JSONObject(it) }
                    val dataArray = jsonObject?.getJSONArray("data")

                    val values = ArrayList<String>()
                    val ids = HashMap<String, Int>()
                    val descriptions = HashMap<String, String>()

                    // Add "Choose an option" to the list of values
                    values.add("No batch")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val batchId = item.optString("batch_id")
                            val shortDescription = item.optString("short_description")
                            val value = "$batchId,\n$shortDescription"
                            val id = item.optInt("id")
                            val description = item.optString("description")
                            val batchType = item.optInt("batch_type")
                            if (batchType == 7) {
                                values.add(value)
                                ids[value] = id
                                descriptions[value] = description
                            }
                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@LabPrepActivity,
                            R.layout.spinner_list,
                            values
                        )
                        adapter.setDropDownViewResource(R.layout.spinner_list)
                        batchSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        batchSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                @SuppressLint("SetTextI18n")
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                                        val selectedValue = values[position]
                                        batchId = ids[selectedValue].toString().toInt()
                                        batchDescription = descriptions[selectedValue].toString()
                                        batchDescriptionText.visibility = View.VISIBLE
                                        batchDescriptionText.text = batchDescription
                                        isBatchSelected = true

                                    } else {
                                        batchDescriptionText.visibility = View.GONE
                                        isBatchSelected = false
                                    }
                                }

                                @SuppressLint("SetTextI18n")
                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    batchDescriptionText.visibility = View.GONE
                                    isBatchSelected = false
                                }
                            }
                    }
                } else {
                    showToast("Connection error")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("$e")
            }
        }
    }

    private fun manageScan() {
        if (isContainerScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                containerId = DatabaseManager.getContainerIdIfValid(scanButtonContainer.text.toString(), false)
                val places = DatabaseManager.checkContainerLoad(containerId)
                containerModelId = DatabaseManager.getContainerModelId(containerId)
                withContext(Dispatchers.Main) {
                    handleContainerScan(places)
                }
            }
        } else if (isObjectScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val sample = scanButtonSample.text.toString()
                sampleContainerId = DatabaseManager.getContainerIdIfValid(sample, true)
                sampleContainerModelId = DatabaseManager.getContainerModelId(sampleContainerId)
                withContext(Dispatchers.Main) {
                    handleObjectScan(sample)
                }
            }
        }
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

    @SuppressLint("SetTextI18n")
    fun handleObjectScan(sample: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val isPairLegal = DatabaseManager.checkContainerHierarchy(containerModelId, sampleContainerModelId)
            if (isPairLegal) {
                withContext(Dispatchers.IO) {
                    sendDataToDirectus(sampleContainerId, containerId, sample)
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
        sample: Int,
        containerId: Int,
        sampleId: String
    ) {
        // Perform the PATCH request to add the values on directus
        try {
            // Retrieve primary keys, token and URL
            val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers/$sampleId"
            val accessToken = DatabaseManager.getAccessToken()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("container_model", sampleContainerModelId)
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
                // Retrieve primary keys, token and URL
                val collectionUrlData = DatabaseManager.getInstance() + "/items/Dried_Samples_Data"

                val clientData = OkHttpClient()

                val jsonBodyData = JSONObject().apply {
                    put("sample_container", sampleId)
                    put("parent_container", containerId)
                    put("status", "present")
                    if (isBatchSelected) {
                        put("batch", batchId)
                    }
                }

                val requestBodyData = jsonBodyData.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val requestData = Request.Builder()
                    .url(collectionUrlData)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(requestBodyData)
                    .build()

                val responseData = clientData.newCall(requestData).execute()

                withContext(Dispatchers.Main) {

                    val responseCodeData = responseData.code
                    if (responseCodeData == HttpURLConnection.HTTP_OK) {
                        // Display a Toast with the response message
                        showToast("$sample correctly added to database")

                        // Check if there is still enough place in the container before initiating the QR code reader
                        val places = DatabaseManager.checkContainerLoad(containerId)
                        handleContainerScan(places)
                        if (places > 0) {
                            scanButtonSample.performClick()
                        }
                    } else {
                        emptyPlace.visibility = View.VISIBLE
                        emptyPlace.text =
                            "$sample already added to database.\nIf you want to move it, please use the moving mode."
                    }
                }
            }
        } catch (e: IOException) {
            showToast("Error: $e")
            }
    }

    private fun visibilityManager () {

        if (isQrScannerActive) {
            scanLayout.visibility = View.VISIBLE
        } else {
            scanLayout.visibility = View.GONE
        }
        if (isContainerValid) {
            sampleContainerLayout.visibility = View.VISIBLE
        } else {
            sampleContainerLayout.visibility = View.GONE
        }

        if (isContainerModelFilled) {
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