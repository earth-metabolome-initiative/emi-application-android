package com.commons.emi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection

class LabWeighingActivity : BaseActivity() {

    // Initiate the displayed objects
    private lateinit var unitSpinner: Spinner
    private lateinit var unitLabel: TextView
    private lateinit var tickCheckBox: CheckBox
    private lateinit var infoLabel: TextView
    private lateinit var chooseWeightLabel: TextView
    private lateinit var targetWeightInput: EditText
    private lateinit var targetWeightTolerance: EditText
    private lateinit var scanFalconLabel: TextView
    private lateinit var scanButtonFalcon: Button
    private lateinit var weightInput: EditText
    private lateinit var submitButton: Button

    // Initiate scanner view
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var scanStatus: TextView

    private var choices: List<String> = mutableListOf("choose a unit")
    private var isTargetWeightInputFilled = false
    private var isTargetWeightToleranceFilled = false
    private var multiplication: String = ""
    private var unitId: Int = 0
    private var isObjectScanActive = false
    private var isQrScannerActive = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_weighing
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Weighing screen"

        // Initialize objects views
        unitLabel = findViewById(R.id.unitLabel)
        unitSpinner = findViewById(R.id.unitSpinner)
        tickCheckBox = findViewById(R.id.tickCheckBox)
        infoLabel = findViewById(R.id.infoLabel)
        chooseWeightLabel = findViewById(R.id.chooseWeightLabel)
        targetWeightInput = findViewById(R.id.targetWeightInput)
        targetWeightTolerance = findViewById(R.id.targetWeightTolerance)
        scanFalconLabel = findViewById(R.id.scanFalconLabel)
        scanButtonFalcon = findViewById(R.id.scanButtonFalcon)
        weightInput = findViewById(R.id.weightInput)
        submitButton = findViewById(R.id.submitButton)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Fetch extraction methods and populate the extraction method spinner.
        fetchValuesAndPopulateSpinner()

        // Set the checkbox as checked by default
        tickCheckBox.isChecked = true

        tickCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Do something with the ticked state
            if (isChecked) {
                showToast("checked")
                chooseWeightLabel.visibility = View.VISIBLE
                targetWeightInput.visibility = View.VISIBLE
                targetWeightTolerance.visibility = View.VISIBLE
                scanFalconLabel.visibility = View.INVISIBLE
                scanButtonFalcon.visibility = View.INVISIBLE

            } else {
                showToast("not checked")
                chooseWeightLabel.visibility = View.INVISIBLE
                targetWeightInput.visibility = View.INVISIBLE
                targetWeightTolerance.visibility = View.INVISIBLE
                scanFalconLabel.visibility = View.VISIBLE
                scanButtonFalcon.visibility = View.VISIBLE
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

        // Set up button click listener for Object QR Scanner
        scanButtonFalcon.setOnClickListener {
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(targetWeightInput.windowToken, 0)
            unitSpinner.visibility = View.INVISIBLE
            unitLabel.text = "selected unit: ${unitSpinner.selectedItem}"
            tickCheckBox.visibility = View.INVISIBLE
            chooseWeightLabel.visibility = View.INVISIBLE
            isObjectScanActive = true
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan falcon"
            flashlightButton.visibility = View.VISIBLE
            targetWeightInput.visibility = View.INVISIBLE
            targetWeightTolerance.visibility = View.INVISIBLE
            scanButtonFalcon.visibility = View.INVISIBLE
            submitButton.visibility = View.INVISIBLE
            ScanManager.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                ScanManager.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                scanButtonFalcon.visibility = View.VISIBLE
                scanButtonFalcon.text = "Value"
                weightInput.visibility = View.VISIBLE
                submitButton.visibility = View.VISIBLE
                weightInput.postDelayed({
                    weightInput.requestFocus()
                    showKeyboard()
                }, 200)
                weightInput.text = null
                scanStatus.text = ""
                scanButtonFalcon.text = scannedSample
            }
        }

        // Add a TextWatcher to the numberInput for real-time validation. Permits to constrain the user entry to a 5% error from the target weight.
        weightInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                targetWeightInput.visibility = View.INVISIBLE
                targetWeightTolerance.visibility = View.INVISIBLE
                chooseWeightLabel.visibility = View.INVISIBLE
                if (tickCheckBox.isChecked){
                    val inputText = s.toString()
                    val inputNumber = inputText.toFloatOrNull()
                    val weightNumber = targetWeightInput.text.toString()
                    val tolerance = targetWeightTolerance.text.toString()
                    val smallNumber = weightNumber.toInt() - tolerance.toDouble()
                    val bigNumber = weightNumber.toInt() + tolerance.toDouble()
                    if (inputNumber != null && inputNumber >= smallNumber && inputNumber <= bigNumber) {
                        weightInput.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                        submitButton.visibility = View.VISIBLE // Show submitButton if valid
                    } else {
                        weightInput.setBackgroundResource(android.R.color.holo_red_light) // Set background to red if not valid
                        submitButton.visibility = View.INVISIBLE // Hide submitButton if not valid
                    }
                } else {
                    weightInput.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                    submitButton.visibility = View.VISIBLE // Show submitButton if valid
                }
            }
        })
        submitButton.setOnClickListener {
            submitButton.visibility= View.INVISIBLE
            val inputText = weightInput.text.toString()
            val inputNumber = inputText.toFloatOrNull()
            val unit = unitId
            CoroutineScope(Dispatchers.IO).launch { sendDataToDirectus(scanButtonFalcon.text.toString(), inputNumber.toString(), unit)}}
    }

    // Function to obtain extraction methods from directus and to populate the spinner.
    private fun fetchValuesAndPopulateSpinner() {
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
                                        tickCheckBox.visibility = View.VISIBLE
                                        chooseWeightLabel.visibility = View.VISIBLE
                                        targetWeightInput.visibility = View.VISIBLE
                                        targetWeightTolerance.visibility = View.VISIBLE
                                        val unit = unitSpinner.selectedItem.toString()
                                        multiplication = multiplications[unit].toString()
                                        unitId = ids[unit].toString().toInt()
                                    } else {
                                        tickCheckBox.visibility = View.INVISIBLE
                                        chooseWeightLabel.visibility = View.INVISIBLE
                                        targetWeightInput.visibility = View.INVISIBLE
                                        targetWeightTolerance.visibility = View.INVISIBLE
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {

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
            scanFalconLabel.visibility = View.VISIBLE
            scanButtonFalcon.visibility = View.VISIBLE
            tickCheckBox.visibility = View.INVISIBLE
            chooseWeightLabel.visibility = View.INVISIBLE
            val target = targetWeightInput.text.toString()
            val correctedTarget = target.toInt()*multiplication.toDouble()
            if (correctedTarget <= 0.00001) {
                infoLabel.text = "Advised extraction setup: 1 metal bead, 500µl solvent"
            } else if (correctedTarget in 0.000011..0.00003) {
                infoLabel.text = "Advised extraction setup: 2 metal beads, 1000µl solvent"
            } else if (correctedTarget in 0.000031..0.00005) {
                infoLabel.text = "Advised extraction setup: 3 metal beads, 1500µl solvent"
            } else {
                infoLabel.text = "No advised extraction setup."
            }

        } else {
            scanFalconLabel.visibility = View.INVISIBLE
            scanButtonFalcon.visibility = View.INVISIBLE
            tickCheckBox.visibility = View.VISIBLE
            chooseWeightLabel.visibility = View.VISIBLE
        }
    }

    // Function that permits to control which extracts are already in the database and increment by one to create a unique one
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
    suspend fun sendDataToDirectus(sampleId: String, weight: String, unit: Int) {
        // Define the table url
        val extractId = checkExistenceInDirectus(sampleId)
        val sampleKey = DatabaseManager.getPrimaryKey(sampleId)

        if (extractId != null) {

            try {
                // Retrieve primary keys, token and URL
                val collectionUrl = "${DatabaseManager.getInstance()}/items/Containers"
                val accessToken = DatabaseManager.getAccessToken()

                val client = OkHttpClient()

                val jsonBody = JSONObject().apply {
                    put("container_id", extractId)
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
                            val collectionUrlExt = DatabaseManager.getInstance() + "/items/Extraction_Data"

                            val clientExt = OkHttpClient()

                            val jsonBodyExt = JSONObject().apply {
                                put("sample_container", id)
                                put("parent_sample_container", sampleKey)
                                put("status", "present")
                                put("dried_weight", weight)
                                put("dried_weight_unit", unit)
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
                                    delay(1500)
                                    scanButtonFalcon.performClick()
                                }
                            } else {
                                showToast("Error, $extractId couldn't be added to database.")
                            }

                        } else {
                            showToast("Error, $extractId couldn't be added to database.")
                        }

                    } else {
                        showToast("Error, $sampleId seems to be absent from the database.")
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

    // Function that permits to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()}
    }
}