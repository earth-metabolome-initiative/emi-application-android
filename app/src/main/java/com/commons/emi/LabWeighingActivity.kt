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
import android.widget.CheckBox
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

class LabWeighingActivity : BaseActivity() {

    // Initiate the displayed objects
    private lateinit var unitLayout: View
    private lateinit var unitSpinner: Spinner

    private lateinit var tickLayout: View
    private lateinit var tickCheckBox: CheckBox
    private lateinit var infoLabel: TextView

    private lateinit var constraintLayout: View
    private lateinit var chooseWeightLabel: TextView
    private lateinit var targetWeightInput: EditText
    private lateinit var targetWeightTolerance: EditText

    private lateinit var sampleContainerLayout: View
    private lateinit var textSampleContainer: TextView
    private lateinit var textNewSampleContainer: TextView
    private lateinit var containerModelSpinner: Spinner

    private lateinit var sampleLayout: View
    private lateinit var scanSampleLabel: TextView
    private lateinit var scanButtonSample: Button

    private lateinit var weightLayout: View
    private lateinit var weightInput: EditText
    private lateinit var submitButton: Button

    private lateinit var scanLayout: LinearLayout
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var noneButton: Button
    private lateinit var scanStatus: TextView

    // Variables
    private var choices: List<String> = mutableListOf("choose a unit")
    private var multiplication: String = ""
    private var unitId: Int = 0
    private var sampleContainerId: Int = 0
    private var sampleContainer: String = ""
    private var sampleContainerModelId: Int = 0
    private var weight: Double = 0.0

    // Trackers
    private var isUnitSelected = false
    private var isConstraintActive = false
    private var isConstraintValid = false
    private var isTargetWeightInputFilled = false
    private var isTargetWeightToleranceFilled = false
    private var isContainerModelFilled = false
    private var isObjectScanActive = false
    private var isQrScannerActive = false
    private var isObjectValid = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_weighing
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPrinterConnection()

        title = "Weighing screen"

        // Initialize objects views
        unitLayout = findViewById(R.id.unitLayout)
        unitSpinner = findViewById(R.id.unitSpinner)

        tickLayout = findViewById(R.id.tickLayout)
        tickCheckBox = findViewById(R.id.tickCheckBox)
        infoLabel = findViewById(R.id.infoLabel)

        constraintLayout = findViewById(R.id.constraintLayout)
        chooseWeightLabel = findViewById(R.id.chooseWeightLabel)
        targetWeightInput = findViewById(R.id.targetWeightInput)
        targetWeightTolerance = findViewById(R.id.targetWeightTolerance)

        sampleContainerLayout = findViewById(R.id.sampleContainerLayout)
        textSampleContainer = findViewById(R.id.textSampleContainer)
        textNewSampleContainer = findViewById(R.id.textNewSampleContainer)
        containerModelSpinner = findViewById(R.id.containerModelSpinner)

        sampleLayout = findViewById(R.id.sampleLayout)
        scanSampleLabel = findViewById(R.id.scanSampleLabel)
        scanButtonSample = findViewById(R.id.scanButtonSample)

        weightLayout = findViewById(R.id.weightLayout)
        weightInput = findViewById(R.id.weightInput)
        submitButton = findViewById(R.id.submitButton)

        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        closeButton = findViewById(R.id.closeButton)
        noneButton = findViewById(R.id.noneButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Fetch extraction methods and populate the extraction method spinner.
        fetchValuesAndPopulateUnitSpinner()

        tickCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Do something with the ticked state
            if (isChecked) {
                isConstraintActive = true
                isConstraintValid = false
                visibilityManager()

            } else {
                isConstraintActive = false
                targetWeightInput.text = null
                targetWeightTolerance.text = null
                infoLabel.text = ""
                isConstraintValid = true
                visibilityManager()
            }
        }

        targetWeightInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                isTargetWeightInputFilled = inputText.isNotEmpty() && inputText.toDoubleOrNull() != null
                updateButtonVisibility()
            }
        })

        targetWeightTolerance.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                isTargetWeightToleranceFilled = inputText.isNotEmpty() && inputText.toDoubleOrNull() != null
                updateButtonVisibility()
            }
        })

        // Fetch container models and populate the container models spinner.
        fetchValuesAndPopulateContainerModelsSpinner()

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
        Log.d("Spannable", "spannable: $spannableString, length: ${spannableString.length}")
        spannableString.setSpan(clickableSpan, 0, spannableString.length, spannableString.length)
        linkTextView.text = spannableString
        linkTextView.movementMethod = LinkMovementMethod.getInstance()

        // Set up button click listener for Object QR Scanner
        scanButtonSample.setOnClickListener {
            // Hide keyboard
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(targetWeightInput.windowToken, 0)

            isObjectScanActive = true
            isQrScannerActive = true
            visibilityManager()
            scanStatus.text = "Scan falcon"
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

        // Add a TextWatcher to the numberInput for real-time validation. Permits to constrain the user entry to a 5% error from the target weight.
        weightInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isConstraintActive){
                    val inputText = s.toString()
                    val inputNumber = inputText.toFloatOrNull()
                    val weightNumber = targetWeightInput.text.toString()
                    val tolerance = targetWeightTolerance.text.toString()
                    val smallNumber = weightNumber.toInt() - tolerance.toDouble()
                    val bigNumber = weightNumber.toInt() + tolerance.toDouble()
                    if (inputNumber != null && inputNumber >= smallNumber && inputNumber <= bigNumber) {
                        weightInput.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                        submitButton.visibility = View.VISIBLE
                    } else {
                        weightInput.setBackgroundResource(android.R.color.holo_red_light) // Set background to red if not valid
                        submitButton.visibility = View.GONE // Hide submitButton if not valid
                    }
                } else {
                    weightInput.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                    submitButton.visibility = View.VISIBLE // Show submitButton if valid
                }
            }
        })

        submitButton.setOnClickListener {
            weight = weightInput.text.toString().toDouble()
            submitButton.visibility= View.GONE
            CoroutineScope(Dispatchers.IO).launch {
                sendDataToDirectus()
            }
        }
    }

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
                    values.add("Choose a unit")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            if (item.getString("base_unit") == "kilogram") {
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
                            this@LabWeighingActivity,
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
                                    if (position > 0) {
                                        isUnitSelected = true
                                        isConstraintActive = true
                                        visibilityManager()
                                        val unit = unitSpinner.selectedItem.toString()
                                        multiplication = multiplications[unit].toString()
                                        unitId = ids[unit].toString().toInt()
                                    } else {
                                        isUnitSelected = false
                                        isConstraintActive = false
                                        visibilityManager()
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    isUnitSelected = false
                                    isConstraintActive = false
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
    private fun updateButtonVisibility() {
        if (isTargetWeightInputFilled && isTargetWeightToleranceFilled) {
            isConstraintValid = true
            visibilityManager()
            val target = targetWeightInput.text.toString()
            val correctedTarget = target.toInt()*multiplication.toDouble()
            if (correctedTarget <= 0.00001) {
                infoLabel.text = "Advised extraction setup:\n- 1 metal bead\n- 500µl solvent"
            } else if (correctedTarget in 0.000011..0.00003) {
                infoLabel.text = "Advised extraction setup:\n- 2 metal beads\n- 1000µl solvent"
            } else if (correctedTarget in 0.000031..0.00005) {
                infoLabel.text = "Advised extraction setup:\n- 3 metal beads\n- 1500µl solvent"
            } else {
                infoLabel.text = "No advised extraction setup."
            }

        } else {
            infoLabel.text = ""
            isConstraintValid = false
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
                            this@LabWeighingActivity,
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

    @SuppressLint("SetTextI18n")
    private fun manageScan() {
        if (isObjectScanActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val sample = scanButtonSample.text.toString()
                sampleContainerId = DatabaseManager.getContainerIdIfValid(sample, true)
                if (sampleContainerId > 0) {
                    scanButtonSample.setTextColor(Color.WHITE)
                    sampleContainerModelId = DatabaseManager.getContainerModelId(sampleContainerId)
                    sampleContainer = sample
                    isObjectValid = true
                    visibilityManager()

                    // show keyboard
                    weightInput.postDelayed({
                        weightInput.requestFocus()
                        showKeyboard()
                    }, 200)
                    weightInput.text = null
                } else {
                    withContext(Dispatchers.Main) {
                        scanButtonSample.setTextColor(Color.RED)
                        scanButtonSample.text = "Invalid sample"
                    }
                }
            }
        }
    }

    // Function that permits to control which extracts are already in the database and increment by one to create a unique one
    @SuppressLint("DefaultLocale")
    private fun checkExistenceInDirectus(sampleId: String): String? {
        for (i in 1..99) {
            val testId = "${sampleId}_${String.format("%02d", i)}"
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

    // Function to send data to Directus
    @SuppressLint("DiscouragedApi")
    suspend fun sendDataToDirectus() {
        // Define the table url
        val extractId = checkExistenceInDirectus(submitButton.text.toString())

        if (extractId != null) {

            try {
                // Retrieve primary keys, token and URL
                val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers"
                val accessToken = DatabaseManager.getAccessToken()

                val client = OkHttpClient()

                val jsonBody = JSONObject().apply {
                    put("container_id", extractId)
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
                            val collectionUrlExt = DatabaseManager.getInstance() + "/items/Extraction_Data"

                            val clientExt = OkHttpClient()

                            val jsonBodyExt = JSONObject().apply {
                                put("sample_container", id)
                                put("parent_sample_container", sampleContainerId)
                                put("status", "present")
                                put("dried_weight", weight)
                                put("extraction_container", sampleContainerModelId)
                                put("dried_weight_unit", unitId)
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
                                printLabel(extractId)
                                // Start a coroutine to delay the next scan by 5 seconds
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(2000)
                                    scanButtonSample.performClick()
                                }
                            } else {
                                showToast("Error, $extractId couldn't be added to database.")
                                isObjectValid = false
                                visibilityManager()
                            }

                        } else {
                            showToast("Error, $extractId couldn't be added to database.")
                        }

                    } else {
                        showToast("Error, $sampleContainer seems to be absent from the database.")
                    }
            } catch (e: IOException) {
                showToast("Error: $e")
            }
        } else {
            showToast("No more available extraction labels")
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
        val prefix = "temp_${parts[0]}_"
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

    // Function that permits to automatically show the keyboard to enter sample weight.
    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(weightInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun visibilityManager () {
        runOnUiThread {
            if (isUnitSelected) {
                tickLayout.visibility = View.VISIBLE
            } else {
                tickLayout.visibility = View.GONE

            }
            if (isConstraintActive) {
                constraintLayout.visibility = View.VISIBLE
            } else {
                constraintLayout.visibility = View.GONE
            }
            if (isConstraintValid) {
                sampleContainerLayout.visibility = View.VISIBLE
            } else {
                sampleContainerLayout.visibility = View.GONE
            }
            if (isContainerModelFilled) {
                sampleLayout.visibility = View.VISIBLE
            } else {
                sampleLayout.visibility = View.GONE
            }
            if (isQrScannerActive) {
                scanLayout.visibility = View.VISIBLE
            } else {
                scanLayout.visibility = View.GONE
            }
            if (isObjectScanActive) {
                tickLayout.visibility = View.GONE
                constraintLayout.visibility = View.GONE
            }
            if (isObjectValid) {
                weightLayout.visibility = View.VISIBLE
            } else {
                weightLayout.visibility = View.GONE
            }
        }
    }

    // Function that permits to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()}
    }
}