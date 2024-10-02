package com.commons.emi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import com.bradysdk.api.printerconnection.CutOption
import com.bradysdk.api.printerconnection.PrintingOptions
import com.bradysdk.printengine.templateinterface.TemplateFactory
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
class LabExtractionActivity : BaseActivity() {

    // Initialize views
    private lateinit var methodLayout: View
    private lateinit var extractionMethodLabel: TextView
    private lateinit var newExtractionMethodLabel: TextView
    private lateinit var extractionMethodSpinner: Spinner
    private lateinit var extractionMethodDescription: TextView

    private lateinit var volumeLayout: View
    private lateinit var solventVolumeLabel: TextView
    private lateinit var solventVolume: EditText
    private lateinit var unitSpinner: Spinner

    private lateinit var batchLayout: View
    private lateinit var scanButtonBatchLabel: TextView
    private lateinit var scanButtonBatch: Button

    private lateinit var containerLayout: View
    private lateinit var scanButtonContainerLabel: TextView
    private lateinit var scanButtonContainer: Button
    private lateinit var containerEmptyPlace: TextView

    private lateinit var extractLayout: View
    private lateinit var scanButtonExtractLabel: TextView
    private lateinit var scanButtonExtract: Button

    private lateinit var scanLayout: LinearLayout
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var noneButton: Button
    private lateinit var scanStatus: TextView

    // Define variables
    private var choices: List<String> = mutableListOf("Choose an option")
    private var multiplication: String = ""
    private var unitId: Int = 0
    private var methodId: Int = 0
    private var batchId: Int = 0

    // Define trackers
    private var isMethodFilled = false
    private var isVolumeFilled = false
    private var isUnitFilled = false
    private var isQrScannerActive = false
    private var isBatchActive = false
    private var isBatchValid = false
    private var isContainerScanActive = false
    private var isContainerValid = false
    private var isObjectScanActive = false
    private var isObjectValid = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_extraction
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Extraction screen"

        // Initialize views
        methodLayout = findViewById(R.id.methodLayout)
        extractionMethodLabel = findViewById(R.id.extractionMethodLabel)
        newExtractionMethodLabel = findViewById(R.id.newExtractionMethodLabel)
        extractionMethodSpinner = findViewById(R.id.extractionMethodSpinner)
        extractionMethodDescription = findViewById(R.id.extractionMethodDescription)

        volumeLayout = findViewById(R.id.volumeLayout)
        solventVolumeLabel = findViewById(R.id.solventVolumeLabel)
        solventVolume = findViewById(R.id.solventVolume)
        unitSpinner = findViewById(R.id.unitSpinner)

        containerLayout = findViewById(R.id.containerLayout)
        scanButtonContainerLabel = findViewById(R.id.scanButtonContainerLabel)
        scanButtonContainer = findViewById(R.id.scanButtonContainer)
        containerEmptyPlace = findViewById(R.id.containerEmptyPlace)

        batchLayout = findViewById(R.id.batchLayout)
        scanButtonBatchLabel = findViewById(R.id.scanButtonBatchLabel)
        scanButtonBatch = findViewById(R.id.scanButtonBatch)

        extractLayout = findViewById(R.id.extractLayout)
        scanButtonExtractLabel = findViewById(R.id.scanButtonExtractLabel)
        scanButtonExtract = findViewById(R.id.scanButtonExtract)

        // Access the included QR scanner views
        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        closeButton = findViewById(R.id.closeButton)
        noneButton = findViewById(R.id.noneButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Make the link clickable for information text to create a new extraction method.
        val linkTextView: TextView = newExtractionMethodLabel
        val spannableString = SpannableString(linkTextView.text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "${DatabaseManager.getInstance()}/admin/content/Extraction_Methods/+"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }
        spannableString.setSpan(clickableSpan, 56, 60, spannableString.length)
        linkTextView.text = spannableString
        linkTextView.movementMethod = LinkMovementMethod.getInstance()

        // Fetch extraction methods and populate the extraction method spinner.
        fetchValuesAndPopulateExtractionMethodSpinner()

        // Asks the user to enter solvent volume and makes scan button for Container visible when a volume is entered.
        solventVolume.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val inputVolume = inputText.toDouble()

                if (inputVolume > 0) {
                    isVolumeFilled = true
                    visibilityManager()
                } else {
                    isVolumeFilled = true
                    visibilityManager()
                }
            }
        })

        // Fetch extraction methods and populate the extraction method spinner.
        fetchValuesAndPopulateUnitSpinner()

        // Set up button click listener for Batch QR Scanner
        scanButtonBatch.setOnClickListener {
            isBatchActive = true
            isContainerScanActive = false
            isObjectScanActive = false
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan batch"
            ScanManager.initialize(this, previewView, flashlightButton) { scannedBatch ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonBatch.text = scannedBatch
                manageScan()
            }
        }

        // Set up button click listener for Container QR Scanner
        scanButtonContainer.setOnClickListener {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(solventVolume.windowToken, 0)
            isContainerScanActive = true
            isObjectScanActive = false
            isBatchActive = false
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan container"
            noneButton.visibility = View.VISIBLE
            ScanManager.initialize(this, previewView, flashlightButton, noneButton) { scannedContainer ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                noneButton.visibility = View.GONE
                scanButtonContainer.text = scannedContainer
                visibilityManager()
                manageScan()
            }
        }

        // Set up button click listener for Object QR Scanner
        scanButtonExtract.setOnClickListener {
            isObjectScanActive = true
            isContainerScanActive = false
            isBatchActive = false
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan extract"
            ScanManager.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonExtract.text = scannedSample
                manageScan()
            }
        }

        closeButton.setOnClickListener {
            isQrScannerActive = false
            visibilityManager()
        }
    }

    // Function to obtain extraction methods from directus and to populate the spinner.
    private fun fetchValuesAndPopulateExtractionMethodSpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val collectionUrl =
                    "${DatabaseManager.getInstance()}/items/Extraction_Methods"
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
                    val descriptions = HashMap<String, String>()
                    val ids = HashMap<String, Int>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose a method")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val value = item.optString("method_name")
                            val description = item.optString("method_description")
                            val id = item.optInt("id")
                            values.add(value)
                            descriptions[value] = description
                            ids[value] = id
                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@LabExtractionActivity,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        extractionMethodSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        extractionMethodSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                                        val selectedValue = values[position]
                                        val selectedDescription = descriptions[selectedValue]
                                        methodId = ids[selectedValue].toString().toInt()
                                        extractionMethodDescription.text = selectedDescription
                                        isMethodFilled = true
                                        visibilityManager()
                                    } else {
                                        isMethodFilled = false
                                        visibilityManager()
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    isMethodFilled = false
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
    private fun fetchValuesAndPopulateUnitSpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val collectionUrl =
                    "${DatabaseManager.getInstance()}/items/SI_Units"
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
                    val multiplications = HashMap<String, Double>()
                    val ids = HashMap<String, Int>()

                    // Add "Choose an option" to the list of values
                    values.add("choose a unit")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            if (item.getString("base_unit") == "liter") {
                                val value = item.optString("unit_name")
                                val multiplication = item.optDouble("multiplication_factor")
                                val id = item.optInt("id")
                                values.add(value)
                                multiplications[value] = multiplication
                                ids[value] = id
                            }

                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@LabExtractionActivity,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        unitSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        unitSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                                        val unit = unitSpinner.selectedItem.toString()
                                        multiplication = multiplications[unit].toString()
                                        unitId = ids[unit].toString().toInt()
                                        isUnitFilled = true
                                        visibilityManager()
                                    } else {
                                        isUnitFilled = false
                                        visibilityManager()
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    isUnitFilled = false
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

    // Function to store data from QR scanner.
    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    @Deprecated("Deprecated in Java")
    fun manageScan() {
        if (isBatchActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val batch = scanButtonBatch.text.toString()
                batchId = DatabaseManager.checkBatch(batch, "extracts")
                withContext(Dispatchers.Main) {
                    handleBatchScan(batchId, batch)
                }
            }
        } else if (isContainerScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val places = DatabaseManager.checkContainer(scanButtonContainer.text.toString())
                withContext(Dispatchers.Main) {
                    handleContainerScan(places)
                }
            }
        } else if (isObjectScanActive) {
            val containerId = scanButtonContainer.text.toString()
            val extractId = scanButtonExtract.text.toString()
            handleObjectScan(containerId, extractId)
        }
    }

    private fun handleBatchScan(batchId: Int, batch: String) {
        when (batchId) {
            in 1..Int.MAX_VALUE -> updateUIForValidBatch()
            -1 -> updateUIForInvalidBatch(batch)
            -2 -> updateUIForNonExitingBatch(batch)
            else -> updateUIForUnknownError()
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForValidBatch() {
        isBatchValid = true
        visibilityManager()
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForInvalidBatch(batch: String) {
        isBatchValid = false
        visibilityManager()
        showToast("$batch is not a valid extraction batch.")
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForNonExitingBatch(batch: String) {
        isBatchValid = false
        visibilityManager()
        showToast("$batch doesn't exit in the database.")
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForUnknownError() {
        isMethodFilled = false
        isVolumeFilled = false
        isUnitFilled = false
        isBatchValid = false
        isContainerValid = false
        isObjectValid = false
        visibilityManager()
        showToast("Unknown error, please reopen the application.")
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
        containerEmptyPlace.setTextColor(Color.GRAY)
        containerEmptyPlace.text = "This container should still contain $places empty places"
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForFullContainer() {
        isContainerValid = false
        visibilityManager()
        containerEmptyPlace.setTextColor(Color.RED)
        containerEmptyPlace.text = "This container is full, please scan another one"
        scanButtonContainer.text = "Value"
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForSampleTubeError() {
        isContainerValid = false
        visibilityManager()
        containerEmptyPlace.setTextColor(Color.RED)
        containerEmptyPlace.text = "You are trying to scan a sample tube, please scan a valid container."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForIndeterminateContainer() {
        isContainerValid = true
        visibilityManager()
        containerEmptyPlace.setTextColor(Color.GRAY)
        containerEmptyPlace.text = "This container is not determined as finite."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForInvalidContainer() {
        isContainerValid = false
        visibilityManager()
        containerEmptyPlace.setTextColor(Color.RED)
        containerEmptyPlace.text = "Invalid container, please scan a valid one."
    }

    @SuppressLint("SetTextI18n")
    fun handleObjectScan(container: String, extract: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val containerModel = DatabaseManager.getContainerModel(container)
            val extractModel = DatabaseManager.getContainerModel(extract)
            val isPairLegal = DatabaseManager.checkContainerHierarchy(containerModel, extractModel)
            if (isPairLegal) {
                val extractId = DatabaseManager.getPrimaryKey(extract)
                val containerId = DatabaseManager.getPrimaryKey(container)
                val volume = solventVolume.text.toString().toDouble()
                withContext(Dispatchers.IO) {
                    sendDataToDirectus(batchId = batchId,
                        containerId = containerId,
                        extract = extract,
                        extractionMethodId = methodId,
                        extractId = extractId,
                        solventVolume = volume,
                        solventVolumeUnitId = unitId
                        )
                }
            } else {
                withContext(Dispatchers.Main) {
                    containerEmptyPlace.visibility = View.VISIBLE
                    containerEmptyPlace.setTextColor(Color.RED)
                    containerEmptyPlace.text =
                        "Invalid pair. You are not allowed to put this child container in this parent container."
                }
            }
        }
    }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private suspend fun sendDataToDirectus(
        extractId: Int,
        extract: String,
        containerId: Int,
        solventVolume: Double,
        solventVolumeUnitId: Int,
        extractionMethodId: Int,
        batchId: Int,
    ) {
        // Perform the PATCH request to add the values on directus
        try {
            val id = DatabaseManager.getExtractionDataPrimaryKey(extractId)
            // Retrieve primary keys, token and URL
            showToast("id: $id")
            val collectionUrl = DatabaseManager.getInstance() + "/items/Extraction_Data/$id"
            Log.d("LabExtractionActivity", "URL: $collectionUrl")
            val accessToken = DatabaseManager.getAccessToken()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("parent_container", containerId)
                put("solvent_volume", solventVolume)
                put("solvent_volume_unit", solventVolumeUnitId)
                put("extraction_method", extractionMethodId)
                put("batch", batchId)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(collectionUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .patch(requestBody)
                .build()

            val response = client.newCall(request).execute()

            withContext(Dispatchers.Main) {

                val responseCode = response.code
                Log.d("LabExtractionActivity", "code: ${response.code}")
                Log.d("LabExtractionActivity", "message: ${response.message}")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Display a Toast with the response message
                    showToast("$extract correctly added to database")

                    printLabel(extract)

                    // Check if there is still enough place in the container before initiating the QR code reader
                    val places =
                        DatabaseManager.checkContainer(scanButtonContainer.text.toString())
                    handleContainerScan(places)
                    if (places > 0) {
                        delay(500)
                        scanButtonExtract.performClick()
                    }
                } else {
                    containerEmptyPlace.visibility = View.VISIBLE
                    containerEmptyPlace.setTextColor(Color.RED)
                    containerEmptyPlace.text =
                        "Error adding $extract to database."
                }
            }
        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    private fun printLabel (label: String) {
        // print label here
        val printerDetails = PrinterManager.printerDetails

        val selectedFileName = when (printerDetails.printerModel) {
            //"M211" -> R.raw.template_m211_extract
            "M511" -> R.raw.template_m511_extract
            else -> throw IllegalArgumentException("${printerDetails.printerModel} is not supported.")
        }

        // Initialize an input stream by directly referencing the raw resource.
        val iStream = resources.openRawResource(selectedFileName)

        val parts = label.split("_")
        val prefix = "${parts[0]}_"
        val code = "${parts[1]}_${parts[2]}"

        // Call the SDK method ".getTemplate()" to retrieve its Template Object
        val template =
            TemplateFactory.getTemplate(iStream, this)
        // Simple way to iterate through any placeholders to set desired values.
        for (placeholder in template.templateData) {
            when (placeholder.name) {
                "QR" -> {
                    placeholder.value = label
                }

                "prefix" -> {
                    placeholder.value = prefix
                }

                "code" -> {
                    placeholder.value = code
                }
            }
        }

        val printingOptions = PrintingOptions()
        printingOptions.cutOption = CutOption.EndOfLabel
        printingOptions.numberOfCopies = 1
        val r = Runnable {
            runOnUiThread {
                printerDetails.print(
                    this,
                    template,
                    printingOptions,
                    null
                )
            }
        }
        val printThread = Thread(r)
        printThread.start()
    }

    private fun visibilityManager () {
        if (isMethodFilled) {
            volumeLayout.visibility = View.VISIBLE
        } else {
            volumeLayout.visibility = View.GONE
        }

        if (isVolumeFilled && isUnitFilled) {
            batchLayout.visibility = View.VISIBLE
        } else {
            batchLayout.visibility = View.GONE
        }

        if (isQrScannerActive) {
            scanLayout.visibility = View.VISIBLE
        } else {
            scanLayout.visibility = View.GONE
        }

        if (isBatchValid) {
            containerLayout.visibility = View.VISIBLE
        } else {
            containerLayout.visibility = View.GONE
        }
        if (isContainerValid) {
            extractLayout.visibility = View.VISIBLE
        } else {
            extractLayout.visibility = View.GONE
        }
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }
}