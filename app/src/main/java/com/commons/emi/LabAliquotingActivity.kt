package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
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

class LabAliquotingActivity : BaseActivity() {

    // Initialize views
    private lateinit var volumeLayout: View
    private lateinit var textAliquotVolume: TextView
    private lateinit var aliquotVolume: EditText
    private lateinit var unitSpinner: Spinner
    
    private lateinit var containerLayout: View
    private lateinit var textContainer: TextView
    private lateinit var scanButtonContainer: Button
    private lateinit var containerEmptyPlace: TextView

    private lateinit var sampleContainerLayout: View
    private lateinit var textSampleContainer: TextView
    private lateinit var textNewSampleContainer: TextView
    private lateinit var containerModelSpinner: Spinner

    private lateinit var aliquotLayout: View
    private lateinit var textBatch: TextView
    private lateinit var batchSpinner: Spinner
    private lateinit var batchDescriptionText: TextView
    private lateinit var textAliquot: TextView
    private lateinit var scanButtonAliquot: Button

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
    private var unitId: Int = 0

    // Define trackers
    private var isVolumeFilled = false
    private var isUnitFilled = false
    private var isQrScannerActive = false
    private var isContainerScanActive = false
    private var isContainerValid = false
    private var isContainerModelFilled = false
    private var isBatchSelected = false
    private var isObjectScanActive = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_aliquoting
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Aliquoting screen"

        // Initialize views
        volumeLayout = findViewById(R.id.volumeLayout)
        textAliquotVolume = findViewById(R.id.textAliquotVolume)
        aliquotVolume = findViewById(R.id.aliquotVolume)
        unitSpinner = findViewById(R.id.unitSpinner)
        
        containerLayout = findViewById(R.id.containerLayout)
        textContainer = findViewById(R.id.textContainer)
        scanButtonContainer = findViewById(R.id.scanButtonContainer)
        containerEmptyPlace = findViewById(R.id.containerEmptyPlace)

        sampleContainerLayout = findViewById(R.id.sampleContainerLayout)
        textSampleContainer = findViewById(R.id.textSampleContainer)
        textNewSampleContainer = findViewById(R.id.textNewSampleContainer)
        containerModelSpinner = findViewById(R.id.containerModelSpinner)
        
        aliquotLayout = findViewById(R.id.aliquotLayout)
        textBatch = findViewById(R.id.textBatch)
        batchSpinner = findViewById(R.id.batchSpinner)
        batchDescriptionText = findViewById(R.id.batchDescriptionText)
        textAliquot = findViewById(R.id.textAliquot)
        scanButtonAliquot = findViewById(R.id.scanButtonAliquot)

        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        closeButton = findViewById(R.id.closeButton)
        noneButton = findViewById(R.id.noneButton)
        scanStatus = findViewById(R.id.scanStatus)

        fetchValuesAndPopulateUnitSpinner()

        // Add a TextWatcher to the numberInput for real-time validation
        aliquotVolume.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val inputNumber = inputText.toFloatOrNull()

                if (inputNumber != null) {
                    isVolumeFilled = true
                    visibilityManager()
                } else {
                    isVolumeFilled = false
                    visibilityManager()
                }
            }
        })

        // Set up button click listener for Container QR Scanner
        scanButtonContainer.setOnClickListener {
            isContainerScanActive = true
            isObjectScanActive = false
            isQrScannerActive = true
            visibilityManager()
            noneButton.visibility = View.VISIBLE
            scanStatus.text = "Scan a container"
            ScanManager.initialize(this, previewView, flashlightButton, noneButton) { scannedContainer ->

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

        // Set up button click listener for Object QR Scanner
        scanButtonAliquot.setOnClickListener {
            isContainerScanActive = false
            isObjectScanActive = true
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan a sample"
            ScanManager.initialize(this, previewView, flashlightButton) { scannedAliquot ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                visibilityManager()
                scanButtonAliquot.textSize = 25f
                scanButtonAliquot.text = scannedAliquot
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
                    val ids = HashMap<String, Int>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose a unit")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            if (item.getString("base_unit") == "liter") {
                                val value = item.optString("unit_name")
                                val id = item.optInt("id")
                                values.add(value)
                                ids[value] = id
                            }

                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@LabAliquotingActivity,
                            R.layout.spinner_list,
                            values
                        )
                        adapter.setDropDownViewResource(R.layout.spinner_list)
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
                            this@LabAliquotingActivity,
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
                            if (batchType == 8) {
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
                            this@LabAliquotingActivity,
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
                val sample = scanButtonAliquot.text.toString()
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
    fun updateUIForUnknownError() {
        isVolumeFilled = false
        isUnitFilled = false
        isContainerValid = false
        visibilityManager()
        showToast("Unknown error, please reopen the application.")
    }
    @SuppressLint("SetTextI18n")
    fun handleObjectScan(sample: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val isPairLegal = DatabaseManager.checkContainerHierarchy(containerModelId, sampleContainerModelId)
            if (isPairLegal) {
                val volume = aliquotVolume.text.toString().toDouble()
                withContext(Dispatchers.IO) {
                    sendDataToDirectus(
                        extractId = sampleContainerId,
                        volume = volume,
                        volumeUnit = unitId,
                        containerId = containerId,
                        extract = sample
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    containerEmptyPlace.setTextColor(Color.RED)
                    containerEmptyPlace.text =
                        "Invalid pair. You are not allowed to put this child container in this parent container."
                }
            }
        }
    }

    // Function to send data to Directus
    private suspend fun sendDataToDirectus(extractId: Int, volume: Double, volumeUnit: Int, containerId: Int, extract: String) {
        // Define the table url
        val aliquot = checkExistenceInDirectus(extract)
        if (aliquot != null) {

            try {
                // Retrieve primary keys, token and URL
                val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers"
                val accessToken = DatabaseManager.getAccessToken()

                val client = OkHttpClient()

                val jsonBody = JSONObject().apply {
                    put("container_id", aliquot)
                    put("container_model", sampleContainerId)
                    put("status", "present")
                    put("reserved", true)
                    put("used", true)
                    put("is_finite", true)
                    put("columns", 1)
                    put("columns_numeric", true)
                    put("rows", 1)
                    put("rows_numeric", true)
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
                    val responseBody = response.body?.string()

                    // Check if response body is not null and parse it
                    if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val dataObject = jsonObject.getJSONObject("data")

                        val aliquotId = dataObject.optInt("id")

                        // Retrieve primary keys, token and URL
                        val collectionUrlExt = DatabaseManager.getInstance() + "/items/Aliquoting_Data"

                        val clientExt = OkHttpClient()

                        val jsonBodyExt = JSONObject().apply {
                            put("sample_container", aliquotId)
                            put("parent_sample_container", extractId)
                            put("parent_container", containerId)
                            put("aliquot_volume", volume)
                            put("aliquot_volume_Unit", volumeUnit)
                            put("status", "present")
                        }

                        val requestBodyExt = jsonBodyExt.toString()
                            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                        val requestExt = Request.Builder()
                            .url(collectionUrlExt)
                            .addHeader("Authorization", "Bearer $accessToken")
                            .post(requestBodyExt)
                            .build()

                        val responseExt = clientExt.newCall(requestExt).execute()

                        val responseCodeExt = responseExt.code
                        if (responseCodeExt == HttpURLConnection.HTTP_OK) {
                            // 'response' contains the response from the server
                            showToast("$extractId correctly added to database")
                            printLabel(aliquot)
                            // Start a coroutine to delay the next scan by 5 seconds
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1500)
                                scanButtonAliquot.performClick()
                            }
                        } else {
                            showToast("Error, $aliquot couldn't be added to database.")
                        }

                    } else {
                        showToast("Error, $aliquot couldn't be added to database.")
                    }

                } else {
                    showToast("Error, $extract seems to be absent from the database.")
                }
            } catch (e: IOException) {
                showToast("Error: $e")
            }
        } else {
            showToast("No more available extraction labels")
        }
    }

    // Function that permits to control which extracts are already in the database and increment by one to create a unique one
    @SuppressLint("DefaultLocale")
    private fun checkExistenceInDirectus(extract: String): String? {
        for (i in 1..99) {
            val testId = "${extract}_${String.format("%02d", i)}"
            try {
                val collectionUrl =
                    "${DatabaseManager.getInstance()}/items/Containers?filter[container_id][_eq]=$testId"
                val client = OkHttpClient()
                val request = Request.Builder()
                    .addHeader("Accept", "application/json")
                    .url(collectionUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == HttpURLConnection.HTTP_OK) {
                    val responseBody = response.body?.string()

                    // Check if response body is not null and parse it
                    if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val dataArray = jsonObject.getJSONArray("data")

                        if (dataArray.length() == 0) {
                            return testId
                        }
                    }
                }
            } catch (e: IOException) {
                return null
            }
        }

        return null
    }

    private fun printLabel (label: String) {
        // print label here
        val printerDetails = PrinterManager.printerDetails

        val selectedFileName = when (printerDetails.printerModel) {
            //"M211" -> R.raw.template_m211_aliquot
            "M511" -> R.raw.template_m511_aliquot
            else -> throw IllegalArgumentException("${printerDetails.printerModel} is not supported.")
        }

        // Initialize an input stream by directly referencing the raw resource.
        val iStream = resources.openRawResource(selectedFileName)

        val parts = label.split("_")
        val prefix = "${parts[0]}_"
        val code = parts[1]
        val suffix = "_${parts[2]}_${parts[3]}"

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

                "suffix" -> {
                    placeholder.value = suffix
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
        if (isVolumeFilled && isUnitFilled) {
            containerLayout.visibility = View.VISIBLE
        } else {
            containerLayout.visibility = View.GONE
        }
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
            aliquotLayout.visibility = View.VISIBLE
        } else {
            aliquotLayout.visibility = View.GONE
        }
    }

    // function to facilitate toasts generation.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}