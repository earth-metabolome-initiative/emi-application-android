package com.commons.emi

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bradysdk.api.printerconnection.CutOption
import com.bradysdk.api.printerconnection.PrintingOptions
import com.bradysdk.api.printerconnection.PrintingStatus
import com.bradysdk.api.templates.Template
import com.bradysdk.printengine.templateinterface.TemplateFactory
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
import java.io.InputStream
import java.net.HttpURLConnection

class MyViewModel : ViewModel() {
    val text = MutableLiveData<String>()
}

class LabBatchActivity : BaseActivity() {

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_lab_batch
    }

    private lateinit var batchText: TextView
    private lateinit var driedBatchButton: Button
    private lateinit var extractionBatchButton: Button
    private lateinit var aliquotBatchButton: Button
    private lateinit var myViewModel: MyViewModel

    // Function that is launched when class is called.
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPrinterConnection()

        title = "Batch screen"

        batchText = findViewById(R.id.batchText)
        driedBatchButton = findViewById(R.id.driedBatchButton)
        extractionBatchButton = findViewById(R.id.extractionBatchButton)
        aliquotBatchButton = findViewById(R.id.aliquotBatchButton)
        myViewModel = ViewModelProvider(this)[MyViewModel::class.java]

        // Set up button click listeners here
        driedBatchButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                createDriedBatch()
            }
        }

        // Observe changes to the ViewModel's text LiveData
        myViewModel.text.observe(this) { newText ->
            batchText.text = newText
        }

        // Set up button click listeners here
        extractionBatchButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                createExtractionBatch()
            }
        }

        // Set up button click listeners here
        aliquotBatchButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                createAliquotBatch()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun createDriedBatch() {
        try {

            // Retrieve token and URL
            val collectionUrl = DatabaseManager.getInstance() + "/items/Batches"
            val accessToken = DatabaseManager.getAccessToken()
            val newBatchId = DatabaseManager.getNewBatch()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("batch_id", newBatchId)
                put("status", "ok")
                put("batch_type", 7)
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
                    batchText.visibility = View.VISIBLE
                    myViewModel.text.value = "sample batch: $newBatchId"
                    managePrinting(newBatchId, "sample")
                } else {
                    showToast("Batch generation failed, please try again")
                }
            }

        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun createExtractionBatch() {
        try {

            // Retrieve token and URL
            val collectionUrl = DatabaseManager.getInstance() + "/items/Batches"
            val accessToken = DatabaseManager.getAccessToken()
            val newBatchId = DatabaseManager.getNewBatch()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("batch_id", newBatchId)
                put("status", "ok")
                put("batch_type", 5)
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
                    val parts = newBatchId.split("_")
                    val containerId = "blk_${parts[1]}"
                    batchText.visibility = View.VISIBLE
                    myViewModel.text.value = "extraction batch: $newBatchId\nUse $containerId label to make your blank and $newBatchId to identify your solvent batch"
                    DatabaseManager.createContainer(
                        containerId = containerId,
                        containerModel = 5,
                        reserved = true,
                        used = true,
                        isFinite = true,
                        columns = 1,
                        columnsNumeric = true,
                        rows = 1,
                        rowsNumeric = true
                    )
                    managePrinting(newBatchId, "extract")
                } else {
                    showToast("Batch generation failed, please try again")
                }
            }

        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun createAliquotBatch() {
        try {

            // Retrieve token and URL
            val collectionUrl = DatabaseManager.getInstance() + "/items/Batches"
            val accessToken = DatabaseManager.getAccessToken()
            val newBatchId = DatabaseManager.getNewBatch()

            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("batch_id", newBatchId)
                put("status", "ok")
                put("batch_type", 8)
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
                    batchText.visibility = View.VISIBLE
                    myViewModel.text.value = "extraction batch: $newBatchId"
                    managePrinting(newBatchId, "aliquot")
                } else {
                    showToast("Batch generation failed, please try again")
                }
            }

        } catch (e: IOException) {
            showToast("Error: $e")
        }
    }

    // function that permits to easily display temporary messages
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }

    private fun managePrinting (batchId: String, batchType: String) {
        val printerDetails = PrinterManager.printerDetails

        val selectedFileName = when (printerDetails.printerModel) {
            "M211" -> R.raw.template_m511_sample
            "M511" -> R.raw.template_m511_sample
            else -> throw IllegalArgumentException("${printerDetails.printerModel} is not supported.")
        }

        // Initialize an input stream by directly referencing the raw resource.
        val iStream = resources.openRawResource(selectedFileName)

        printBatch(batchId, iStream, batchType)
    }

    private fun printBatch(batchId: String, iStream: InputStream, batchType: String) {
        val printerDetails = PrinterManager.printerDetails
        val parts = batchId.split("_")
        val prefix = parts[0]
        val code = parts[1]

        // Get the template
        val template = TemplateFactory.getTemplate(iStream, this)

        // Set placeholders for the template
        for (placeholder in template.templateData) {
            when (placeholder.name) {
                "QR" -> placeholder.value = batchId
                "prefix" -> placeholder.value = prefix
                "code" -> placeholder.value = code
            }
        }

        // Configure printing options
        val printingOptions = PrintingOptions().apply {
            cutOption = CutOption.EndOfLabel
            numberOfCopies = 1
        }

        // First print job
        val printingStatus = printerDetails.print(this, template, printingOptions, null)

        // Check the printing status
        if (printingStatus == PrintingStatus.PrintingSucceeded) {
            Log.i("Printer", "First print job succeeded")

            // If batchType is "extract", print the blank label
            if (batchType == "extract") {
                printBlank(batchId, template) // Trigger second print
            }
        } else if (printingStatus == PrintingStatus.PrintingFailed) {
            Log.e("Printer", "First print job failed")
            // Handle the print failure (e.g., retry, show error to user)
        } else {
            Log.w("Printer", "Printing in progress or unknown status")
            // Handle other cases like Printing or Unknown if necessary
        }
    }

    private fun printBlank(batchId: String, template: Template) {
        val printerDetails = PrinterManager.printerDetails
        val parts = batchId.split("_")
        val batchBlk = "blk_${parts[1]}"
        val code = "_${parts[1]}"

        // Set placeholders for the template
        for (placeholder in template.templateData) {
            when (placeholder.name) {
                "QR" -> placeholder.value = batchBlk
                "prefix" -> placeholder.value = "blk"
                "code" -> placeholder.value = code
            }
        }

        // Configure printing options
        val printingOptions = PrintingOptions().apply {
            cutOption = CutOption.EndOfLabel
            numberOfCopies = 1
        }

        // Second print job
        val printingStatus = printerDetails.print(this, template, printingOptions, null)

        // Check the printing status of the second print job
        when (printingStatus) {
            PrintingStatus.PrintingSucceeded -> {
                Log.i("Printer", "Second print job succeeded")
            }
            PrintingStatus.PrintingFailed -> {
                Log.e("Printer", "Second print job failed")
                // Handle the print failure
            }
            else -> {
                Log.w("Printer", "Second print in progress or unknown status")
                // Handle other cases like Printing or Unknown if necessary
            }
        }
    }
}