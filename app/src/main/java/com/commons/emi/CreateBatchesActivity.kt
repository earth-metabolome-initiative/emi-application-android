package com.commons.emi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bradysdk.api.printerconnection.CutOption
import com.bradysdk.api.printerconnection.PrintingOptions
import com.bradysdk.api.printerconnection.PrintingStatus
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

class CreateBatchesActivity : BaseActivity() {

    private lateinit var shortDescriptionText: EditText
    private lateinit var descriptionText: EditText
    private lateinit var submitButton: Button

    private var progressOverlay: View? = null
    private var progressBar: ProgressBar? = null
    private var progressMessage: TextView? = null

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_create_batches
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the button's collier number from intent
        val batch = intent.getStringExtra("batch")

        title = "Create $batch batch"

        if (batch == "extraction") {
            checkPrinterConnection()
        }

        shortDescriptionText = findViewById(R.id.shortDescriptionText)
        descriptionText = findViewById(R.id.descriptionText)
        submitButton = findViewById(R.id.submitButton)

        shortDescriptionText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkButtonVisibility()
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        descriptionText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkButtonVisibility()
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        submitButton.setOnClickListener {
            when (batch) {
                "samples" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        createBatch("dried_samples")
                    }
                }
                "extraction" -> {

                    CoroutineScope(Dispatchers.IO).launch {
                        createBatch("extraction")
                    }
                }
                "aliquots" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        createBatch("aliquots")
                    }
                }
            }
        }
    }

    fun checkButtonVisibility() {
        val isShortDescriptionValid = shortDescriptionText.text.length <= 20
        val isDescriptionValid = descriptionText.text.isNotEmpty()

        if (!isShortDescriptionValid) {
            shortDescriptionText.error = "Maximum 20 characters allowed"
        }

        // Show button if both descriptions are valid
        submitButton.visibility = if (isShortDescriptionValid && isDescriptionValid) View.VISIBLE else View.GONE
    }

    private suspend fun createBatch(batchType: String) {
        try {

            val batchTypeId = getBatchTypeId(batchType)

            // Retrieve token and URL
            val collectionUrl = DatabaseManager.getInstance() + "/items/Batches"
            val accessToken = DatabaseManager.getAccessToken()
            val newBatchId = DatabaseManager.getNewBatch()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("batch_id", newBatchId)
                put("short_description", shortDescriptionText.text.toString())
                put("description", descriptionText.text.toString())
                put("status", "ok")
                put("batch_type", batchTypeId)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(collectionUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            withContext(Dispatchers.Main) {

                val responseCode = response.code
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Display a Toast with the response message
                    showToast("$newBatchId correctly added to database")
                    if (batchType == "extraction") {
                        printBatch(newBatchId)
                    } else {
                        finish()
                    }
                } else {
                    showToast("Batch generation failed, please try again")
                }
            }

        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    private suspend fun getBatchTypeId(batchType: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            val collectionUrl = "${DatabaseManager.getInstance()}/items/Batch_Types?filter[batch_type][_eq]=$batchType&&limit=1"

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

                    if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        val id = firstItem.optInt("id")
                        result = id
                        result
                    } else {
                        result = -2 // no data found
                        result
                    }
                } else {
                    result = -3 //response body null
                    result
                }
            } else {
                -4// non 200 response
            }
        } catch (e: IOException) {
            -5 // internet or other errors
        }
    }

    private fun printBatch(batchId: String) {
        runOnUiThread {
            showProgressOverlay()
            updateProgressMessage("Printing solvent label...")
        }
        val printerDetails = PrinterManager.printerDetails

        // Select template for batch print
        val selectedFileNameBatch = when (printerDetails.printerModel) {
            "M511" -> R.raw.template_m511_sample
            else -> throw IllegalArgumentException("${printerDetails.printerModel} is not supported.")
        }

        // Initialize input stream for batch print
        val iStreamBatch = resources.openRawResource(selectedFileNameBatch)

        // Get a fresh template for batch print
        val templateBatch = TemplateFactory.getTemplate(iStreamBatch, this)

        // Extract batch data
        val parts = batchId.split("_")
        val prefix = parts[0]
        val code = parts[1]

        // Set placeholders for the first template
        for (placeholder in templateBatch.templateData) {
            when (placeholder.name) {
                "QR" -> placeholder.value = batchId
                "prefix" -> placeholder.value = prefix
                "code" -> placeholder.value = code
            }
            // Log the placeholder values
            Log.i(placeholder.name, placeholder.value)
        }

        // Configure printing options
        val printingOptionsBatch = PrintingOptions().apply {
            cutOption = CutOption.EndOfLabel
            numberOfCopies = 1
        }

        // Execute first print job
        val printingStatusBatch = printerDetails.print(this, templateBatch, printingOptionsBatch, null)

        // Handle the status of the first print job
        when (printingStatusBatch) {
            PrintingStatus.PrintingSucceeded -> {
                Log.i("Printer", "First print job succeeded")
                iStreamBatch.close()
                //templateBatch.wait()

                CoroutineScope(Dispatchers.IO).launch {

                    delay(5000)
                    // Now print the blank label after success
                    printBlank(batchId) // Trigger second print job
                }
            }
            PrintingStatus.PrintingFailed -> {
                Log.e("Printer", "First print job failed")
            }
            else -> {
                Log.w("Printer", "Printing in progress or unknown status")
            }
        }
    }

    private fun printBlank(batchId: String) {
        runOnUiThread {
            updateProgressMessage("Printing blank label...")
        }

        val printerDetails = PrinterManager.printerDetails

        // Select template for blank print
        val selectedFileNameBlank = when (printerDetails.printerModel) {
            "M511" -> R.raw.template_m511_sample
            else -> throw IllegalArgumentException("${printerDetails.printerModel} is not supported.")
        }

        // Initialize input stream for blank print
        val iStreamBlank = resources.openRawResource(selectedFileNameBlank)

        // Get a fresh template for blank print
        val templateBlank = TemplateFactory.getTemplate(iStreamBlank, this)

        // Extract batch data
        val parts = batchId.split("_")
        val batchBlk = "blk_${parts[1]}"
        val code = "_${parts[1]}"

        // Set placeholders for the second template (blank)
        for (placeholder in templateBlank.templateData) {
            when (placeholder.name) {
                "QR" -> placeholder.value = batchBlk
                "prefix" -> placeholder.value = "blk"
                "code" -> placeholder.value = code
            }
            // Log the placeholder values
            Log.i(placeholder.name, placeholder.value)
        }

        // Configure printing options
        val printingOptionsBlank = PrintingOptions().apply {
            cutOption = CutOption.EndOfLabel
            numberOfCopies = 1
        }

        // Execute second print job
        val printingStatusBlank = printerDetails.print(this, templateBlank, printingOptionsBlank, null)

        // Handle the status of the second print job
        when (printingStatusBlank) {
            PrintingStatus.PrintingSucceeded -> {
                Log.i("Printer", "Second print job succeeded")
                iStreamBlank.close()

                CoroutineScope(Dispatchers.IO).launch {
                    delay(2000)
                    runOnUiThread {
                        hideProgressOverlay()
                    }
                    finish()
                }

            }
            PrintingStatus.PrintingFailed -> {
                Log.e("Printer", "Second print job failed")
            }
            else -> {
                Log.w("Printer", "Printing in progress or unknown status")
            }
        }
    }

    // Function to show the progress overlay
    private fun showProgressOverlay() {
        if (progressOverlay == null) {
            val rootView = findViewById<ViewGroup>(android.R.id.content).rootView as ViewGroup
            progressOverlay = LayoutInflater.from(this).inflate(R.layout.progress_overlay, rootView, false)
            rootView.addView(progressOverlay)
        }
        progressOverlay?.visibility = View.VISIBLE

        // Initialize references to progressBar and progressMessage
        progressBar = progressOverlay?.findViewById(R.id.progressBar)
        progressMessage = progressOverlay?.findViewById(R.id.progressMessage)
    }

    // Function to hide the progress overlay
    private fun hideProgressOverlay() {
        progressOverlay?.visibility = View.GONE
    }

    // Function to update the progress message
    private fun updateProgressMessage(message: String) {
        progressMessage?.text = message
    }

    // function that permits to easily display temporary messages
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }
}