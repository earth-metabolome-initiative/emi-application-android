package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class LogisticAttributingActivity : BaseActivity() {

    // Initiate the displayed objects
    private lateinit var containerModelLayout: View
    private lateinit var textContainerModel: TextView
    private lateinit var textNewContainerModel: TextView
    private lateinit var containerModelSpinner: Spinner

    private lateinit var containerLayout: View
    private lateinit var textContainer: TextView
    private lateinit var scanButtonContainer: Button

    private lateinit var scanLayout: LinearLayout
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var scanStatus: TextView

    // Define variables
    private var choices: List<String> = mutableListOf("Choose an option")
    private var containerModelId: Int = 0
    private var containerId: Int = 0
    private var column: Int = 0
    private var row: Int = 0
    private var columnNumeric: Boolean = false
    private var rowNumeric: Boolean = false

    // Define trackers
    private var isContainerModelFilled = false
    private var isQrScannerActive = false


    override fun getLayoutResourceId(): Int {
        return R.layout.activity_logistic_attributing
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Labels"

        // Define the breadcrumb path for Home
        val breadcrumbs = listOf(
            Pair("Login", LoginActivity::class.java),
            Pair("Home", HomeActivity::class.java),
            Pair("Logistic", HomeLogisticActivity::class.java),
            Pair("Labels", null)
        )

        // Set breadcrumbs in com.bruelhart.coulage.ch.brulhart.farmapp.BaseActivity
        setBreadcrumbs(breadcrumbs)

        // Initialize objects views
        containerModelLayout = findViewById(R.id.containerModelLayout)
        textContainerModel = findViewById(R.id.textContainerModel)
        textNewContainerModel = findViewById(R.id.textNewContainerModel)
        containerModelSpinner = findViewById(R.id.containerModelSpinner)

        containerLayout = findViewById(R.id.containerLayout)
        textContainer = findViewById(R.id.textContainer)
        scanButtonContainer = findViewById(R.id.scanButtonContainer)

        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        closeButton = findViewById(R.id.closeButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Make the link clickable for information text to create a new container model.
        val linkTextView: TextView = textNewContainerModel
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

        // Set up button click listener for Container QR Scanner
        scanButtonContainer.setOnClickListener {
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan a container"
            ScanManager.initialize(this, previewView, flashlightButton) {scannedContainer ->
                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonContainer.textSize = 25f
                scanButtonContainer.setTextColor(Color.WHITE)
                scanButtonContainer.text = scannedContainer
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
                    val columns = HashMap<String, Int>()
                    val rows = HashMap<String, Int>()
                    val columnsNumeric = HashMap<String, Boolean>()
                    val rowsNumeric = HashMap<String, Boolean>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose a container model")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val volume = item.optDouble("volume")
                            val isSampleContainer = item.optBoolean("is_sample_container")
                            if (!isSampleContainer && volume > 0) {
                                val id = item.optInt("id")
                                val containerTypeId = item.optInt("container_type")
                                val containerType = DatabaseManager.getContainerType(containerTypeId)
                                val volumeUnitId = item.optInt("volume_unit")
                                val volumeUnit = DatabaseManager.getUnit(volumeUnitId)
                                val brandId = item.optInt("brand")
                                val brand = DatabaseManager.getBrand(brandId)
                                val column = item.optInt("columns")
                                val row = item.optInt("rows")
                                val columnNumeric = item.optBoolean("columns_numeric")
                                val rowNumeric = item.optBoolean("rows_numeric")
                                val value = "$containerType $volume $volumeUnit $brand"
                                values.add(value)
                                ids[value] = id
                                columns[value] = column
                                rows[value] = row
                                columnsNumeric[value] = columnNumeric
                                rowsNumeric[value] = rowNumeric
                            }
                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@LogisticAttributingActivity,
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
                                        containerModelId = ids[selectedValue].toString().toInt()
                                        column = columns[selectedValue].toString().toInt()
                                        row = rows[selectedValue].toString().toInt()
                                        columnNumeric = columnsNumeric[selectedValue].toString().toBoolean()
                                        rowNumeric = rowsNumeric[selectedValue].toString().toBoolean()
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

    @SuppressLint("SetTextI18n")
    private fun manageScan() {
        if (scanButtonContainer.text.toString().matches(Regex("^container_\\dx\\d_\\d{6}\$")) || scanButtonContainer.text.toString().matches(Regex("^container_\\d{6}\$"))) {
            CoroutineScope(Dispatchers.IO).launch {
                containerId =
                    DatabaseManager.getContainerIdIfNew(scanButtonContainer.text.toString())

                when (containerId) {
                    in 1..Int.MAX_VALUE -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            sendDataToDirectus()
                        }
                    }
                    -1 -> {
                        withContext(Dispatchers.Main) {
                            scanButtonContainer.setTextColor(Color.RED)
                            scanButtonContainer.text = "Container already attributed"

                        }
                    }
                    in -2 downTo -3 -> {
                        withContext(Dispatchers.Main) {
                            scanButtonContainer.setTextColor(Color.RED)
                            scanButtonContainer.text = "Container not found"
                        }
                    }
                    -4 -> showToast("Database error, please try again")
                    -5 -> showToast("Please check your internet connection")
                }
            }
        } else {
            scanButtonContainer.setTextColor(Color.RED)
            scanButtonContainer.text = "Invalid container"
        }
    }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private fun sendDataToDirectus() {
        // Perform the PATCH request to add the values on directus
        try {
            // Retrieve primary keys, token and URL
            val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers/$containerId"
            val accessToken = DatabaseManager.getAccessToken()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("container_model", containerModelId)
                put("used", true)
                put("is_finite", true)
                put("columns", column)
                put("columns_numeric", columnNumeric)
                put("rows", row)
                put("rows_numeric", rowNumeric)

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
                showToast("${scanButtonContainer.text} correctly associated to container")
                runOnUiThread { scanButtonContainer.performClick() }
            } else {
                showToast("Database error: ${response.message}")
            }
        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    private fun visibilityManager () {
        if (isContainerModelFilled) {
            containerLayout.visibility = View.VISIBLE
        } else {
            containerLayout.visibility = View.GONE
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