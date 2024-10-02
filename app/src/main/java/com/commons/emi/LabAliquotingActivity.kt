package com.commons.emi

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
class LabAliquotingActivity : BaseActivity() {

    // Initialize views
    private lateinit var volumeLayout: View
    private lateinit var aliquotVolumeLabel: TextView
    private lateinit var aliquotVolume: EditText
    private lateinit var unitSpinner: Spinner
    
    private lateinit var containerLayout: View
    private lateinit var scanButtonContainerLabel: TextView
    private lateinit var scanButtonContainer: Button
    private lateinit var containerEmptyPlace: TextView

    private lateinit var aliquotLayout: View
    private lateinit var scanButtonAliquotLabel: TextView
    private lateinit var scanButtonAliquot: Button
    
    private lateinit var scanLayout: View
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var scanStatus: TextView

    // Define variables
    private var choices: List<String> = mutableListOf("Choose an option")
    private var unitId: Int = 0

    // Define trackers
    private var isVolumeFilled = false
    private var isUnitFilled = false
    private var isQrScannerActive = false
    private var isContainerScanActive = false
    private var isContainerValid = false
    private var isObjectScanActive = false
    private var isObjectValid = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_aliquoting
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Aliquoting screen"

        // Initialize views
        volumeLayout = findViewById(R.id.volumeLayout)
        aliquotVolumeLabel = findViewById(R.id.aliquotVolumeLabel)
        aliquotVolume = findViewById(R.id.aliquotVolume)
        unitSpinner = findViewById(R.id.unitSpinner)
        
        containerLayout = findViewById(R.id.containerLayout)
        scanButtonContainerLabel = findViewById(R.id.scanButtonContainerLabel)
        scanButtonContainer = findViewById(R.id.scanButtonContainer)
        containerEmptyPlace = findViewById(R.id.containerEmptyPlace)
        
        aliquotLayout = findViewById(R.id.aliquotLayout)
        scanButtonAliquotLabel = findViewById(R.id.scanButtonAliquotLabel)
        scanButtonAliquot = findViewById(R.id.scanButtonAliquot)

        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
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
            scanStatus.text = "Scan container"
            visibilityManager()
            ScanManager.initialize(this, previewView, flashlightButton) { scannedContainer ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                scanButtonContainer.text = scannedContainer
                visibilityManager()
                manageScan()
            }
        }

        // Set up button click listener for Object QR Scanner
        scanButtonAliquot.setOnClickListener {
            isContainerScanActive = false
            isObjectScanActive = true
            isQrScannerActive = true
            scanStatus.text = "Scan vials"
            visibilityManager()
            ScanManager.initialize(this, previewView, flashlightButton) { scannedAliquot ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                scanButtonAliquot.text = scannedAliquot
                visibilityManager()
                manageScan()
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
                    val ids = HashMap<String, Int>()

                    // Add "Choose an option" to the list of values
                    values.add("choose a unit")

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

    // Store scanned values depending on which scan is performed
    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    fun manageScan() {

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {

                if (isContainerScanActive) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val places = DatabaseManager.checkContainer(scanButtonContainer.text.toString())
                        withContext(Dispatchers.Main) {
                            handleContainerScan(places)
                        }
                    }
                } else if (isObjectScanActive) {
                    val container = scanButtonContainer.text.toString()
                    val aliquot = scanButtonAliquot.text.toString()
                    handleObjectScan(container, aliquot)
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
        containerEmptyPlace.setTextColor(Color.GRAY)
        containerEmptyPlace.text = "This container should still contain $places empty places"
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForFullContainer() {
        isContainerValid = false
        containerEmptyPlace.setTextColor(Color.RED)
        containerEmptyPlace.text = "This container is full, please scan another one"
        scanButtonContainer.text = "Value"
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForSampleTubeError() {
        isContainerValid = false
        containerEmptyPlace.setTextColor(Color.RED)
        containerEmptyPlace.text = "You are trying to scan a sample tube, please scan a valid container."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForIndeterminateContainer() {
        isContainerValid = true
        containerEmptyPlace.setTextColor(Color.GRAY)
        containerEmptyPlace.text = "This container is not determined as finite."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForInvalidContainer() {
        isContainerValid = false
        containerEmptyPlace.setTextColor(Color.RED)
        containerEmptyPlace.text = "Invalid container, please scan a valid one."
    }

    @SuppressLint("SetTextI18n")
    fun updateUIForUnknownError() {
        isVolumeFilled = false
        isUnitFilled = false
        isContainerValid = false
        isObjectValid = false
        visibilityManager()
        showToast("Unknown error, please reopen the application.")
    }
    @SuppressLint("SetTextI18n")
    fun handleObjectScan(container: String, aliquot: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val containerModel = DatabaseManager.getContainerModel(container)
            val aliquotModel = DatabaseManager.getContainerModel(aliquot)
            val isPairLegal = DatabaseManager.checkContainerHierarchy(containerModel, aliquotModel)
            if (isPairLegal) {
                val extract = scanButtonAliquot.text.toString()
                val containerId = DatabaseManager.getPrimaryKey(container)
                val volume = aliquotVolume.text.toString().toDouble()
                withContext(Dispatchers.IO) {
                    sendDataToDirectus(
                        extract = extract,
                        volume = volume,
                        volumeUnit = unitId,
                        containerId = containerId,
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
    private suspend fun sendDataToDirectus(extract: String, volume: Double, volumeUnit: Int, containerId: Int) {
        // Define the table url
        val aliquot = checkExistenceInDirectus(extract)
        val extractId = DatabaseManager.getPrimaryKey(extract)

        if (aliquot != null) {

            try {
                // Retrieve primary keys, token and URL
                val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers"
                val accessToken = DatabaseManager.getAccessToken()

                val client = OkHttpClient()

                val jsonBody = JSONObject().apply {
                    put("container_id", extract)
                    put("container_model", 5) // TODO implement container model choice
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

                        val id = dataObject.optInt("id")

                        // Retrieve primary keys, token and URL
                        val collectionUrlExt = DatabaseManager.getInstance() + "/items/Aliquoting_Data"

                        val clientExt = OkHttpClient()

                        val jsonBodyExt = JSONObject().apply {
                            put("sample_container", id)
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
            aliquotLayout.visibility = View.VISIBLE
        } else {
            aliquotLayout.visibility = View.GONE
        }

        if (isObjectValid) {
            showToast("Object valid")
        } else {
            showToast("Object invalid")
        }
    }

    // function to facilitate toasts generation.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }
}