package com.commons.emi

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.HttpURLConnection

data class LocationEntry(val code: String, val fullPath: String)

class SearchActivity : BaseActivity() {

    private lateinit var searchSpinner: Spinner

    private lateinit var sampleLayout: View
    private lateinit var projectsSpinner: Spinner
    private lateinit var idEntry: EditText
    private lateinit var extractEntry: EditText
    private lateinit var aliquotEntry: EditText

    private lateinit var rangeLayout: View
    private lateinit var textTo: TextView
    private lateinit var idEntryRange: EditText
    private lateinit var extractEntryRange: EditText
    private lateinit var aliquotEntryRange: EditText

    private lateinit var batchLayout: View
    private lateinit var idEntryBatch: EditText

    private lateinit var searchButton: Button

    private lateinit var locationTableLayout: TableLayout

    private var choices: List<String> = mutableListOf("Choose an option")
    private var searchMode = ""
    private var project: String = ""

    private var isSearchModeSelected = false
    private var isIdEntryValid = false
    private var isExtractEntryValid = true
    private var isAliquotEntryValid = true
    private var isIdEntryRangeValid = false
    private var isExtractEntryRangeValid = true
    private var isAliquotEntryRangeValid = true
    private var isIdEntryBatchValid = false

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_search
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Navigation"

        // Define the breadcrumb path for Home
        val breadcrumbs = listOf(
            Pair("Home", HomeActivity::class.java),
            Pair("Navigation", null)
        )

        setBreadcrumbs(breadcrumbs)

        searchSpinner = findViewById(R.id.searchSpinner)

        sampleLayout = findViewById(R.id.sampleLayout)
        projectsSpinner = findViewById(R.id.projectsSpinner)
        idEntry = findViewById(R.id.idEntry)
        extractEntry = findViewById(R.id.extractEntry)
        aliquotEntry = findViewById(R.id.aliquotEntry)

        rangeLayout = findViewById(R.id.rangeLayout)
        textTo = findViewById(R.id.textTo)
        idEntryRange = findViewById(R.id.idEntryRange)
        extractEntryRange = findViewById(R.id.extractEntryRange)
        aliquotEntryRange = findViewById(R.id.aliquotEntryRange)

        batchLayout = findViewById(R.id.batchLayout)
        idEntryBatch = findViewById(R.id.idEntryBatch)

        searchButton = findViewById(R.id.searchButton)

        locationTableLayout = findViewById(R.id.locationTableLayout)

        fetchValuesAndPopulateSearchSpinner()

        searchButton.setOnClickListener {
            when (searchMode) {
                "sample" -> {
                    val code = project + "_" + idEntry.text.toString() + (if (extractEntry.text.toString() != "") "_${extractEntry.text}" else "") + (if (aliquotEntry.text.toString() != "") "_${aliquotEntry.text}" else "")
                    CoroutineScope(Dispatchers.IO).launch {
                        val location = DatabaseManager.getFullPath(code)
                        withContext(Dispatchers.Main) {
                            addRowToTable(code, location)
                        }
                    }
                }
                "range" -> {
                    val startCode = project + "_" + idEntry.text.toString() + (if (extractEntry.text.toString() != "") "_${extractEntry.text}" else "") + (if (aliquotEntry.text.toString() != "") "_${aliquotEntry.text}" else "")
                    val endCode = project + "_" + idEntryRange.text.toString() + (if (extractEntryRange.text.toString() != "") "_${extractEntryRange.text}" else "") + (if (aliquotEntryRange.text.toString() != "") "_${aliquotEntryRange.text}" else "")
                }
                "batch" -> {
                    val batchCode = "batch_$idEntryBatch"
                }
            }
        }
    }

    // Function to obtain extraction methods from directus and to populate the spinner.
    private fun fetchValuesAndPopulateSearchSpinner() {
        val values = listOf("Search mode", "sample", "range", "batch", "species")

        val adapter = ArrayAdapter(
            this@SearchActivity,
            R.layout.spinner_list,
            values
        )
        adapter.setDropDownViewResource(R.layout.spinner_list)
        searchSpinner.adapter = adapter

        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
        searchSpinner.onItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            @SuppressLint("SetTextI18n")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                    isSearchModeSelected = true
                    val selectedValue = values[position]
                    when (selectedValue) {
                        "sample" -> {
                            searchMode = "sample"
                            visibilityManager()
                            sampleSearch()
                        }
                        "range" -> {
                            searchMode = "range"
                            visibilityManager()
                            sampleSearch()
                            rangeSearch()
                        }
                        "batch" -> {
                            searchMode = "batch"
                            visibilityManager()
                            batchSearch()
                        }
                        "species" -> {
                            //searchMode = "species"
                            searchMode = ""
                            visibilityManager()
                            showToast("Not yet implemented")
                        }
                        else -> showToast("error")
                    }
                } else {
                    searchMode = ""
                    visibilityManager()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onNothingSelected(parent: AdapterView<*>?) {
                isSearchModeSelected = false
                searchMode = ""
                visibilityManager()
            }
        }
    }

    private fun sampleSearch () {
        fetchValuesAndPopulateProjectsSpinner()

        idEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 6) {
                    idEntry.setBackgroundColor(Color.WHITE)
                    isIdEntryValid = true
                } else {
                    idEntry.setBackgroundColor(Color.RED)
                    isIdEntryValid = false
                }
                visibilityManager()
            }
        })

        extractEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 2 || length == 0) {
                    extractEntry.setBackgroundColor(Color.WHITE)
                    isExtractEntryValid = true
                } else {
                    extractEntry.setBackgroundColor(Color.RED)
                    isExtractEntryValid = false
                }
                visibilityManager()
            }
        })

        aliquotEntry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 2 || length == 0) {
                    aliquotEntry.setBackgroundColor(Color.WHITE)
                    isAliquotEntryValid = true
                } else {
                    aliquotEntry.setBackgroundColor(Color.RED)
                    isAliquotEntryValid = false
                }
                visibilityManager()
            }
        })
    }

    private fun rangeSearch () {
        idEntryRange.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 6) {
                    idEntryRange.setBackgroundColor(Color.WHITE)
                    isIdEntryRangeValid = true
                } else {
                    idEntryRange.setBackgroundColor(Color.RED)
                    isIdEntryRangeValid = false
                }
                visibilityManager()
            }
        })

        extractEntryRange.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 2 || length == 0) {
                    extractEntryRange.setBackgroundColor(Color.WHITE)
                    isExtractEntryRangeValid = true
                } else {
                    extractEntryRange.setBackgroundColor(Color.RED)
                    isExtractEntryRangeValid = false
                }
                visibilityManager()
            }
        })

        aliquotEntryRange.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 2 || length == 0) {
                    aliquotEntryRange.setBackgroundColor(Color.WHITE)
                    isAliquotEntryRangeValid = true
                } else {
                    aliquotEntryRange.setBackgroundColor(Color.RED)
                    isAliquotEntryRangeValid = false
                }
                visibilityManager()
            }
        })
    }

    private fun batchSearch () {
        idEntryBatch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val length = inputText.length

                if (length == 6) {
                    idEntryBatch.setBackgroundColor(Color.WHITE)
                    isIdEntryBatchValid = true
                } else {
                    idEntryBatch.setBackgroundColor(Color.RED)
                    isIdEntryBatchValid = false
                }
                visibilityManager()
            }
        })
    }

    private fun fetchValuesAndPopulateProjectsSpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val collectionUrl =
                    "${DatabaseManager.getInstance()}/items/Projects?sort=project_id"
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

                    // Add "Choose an option" to the list of values
                    values.add("Project")

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val value = item.optString("project_id")
                            values.add(value)
                        }
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@SearchActivity,
                            R.layout.spinner_list,
                            values
                        )
                        adapter.setDropDownViewResource(R.layout.spinner_list)
                        projectsSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        projectsSpinner.onItemSelectedListener =
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
                                        project = selectedValue
                                        textTo.text = "to $project"
                                    } else {
                                        project = ""
                                        textTo.text = "to project"
                                    }
                                }

                                @SuppressLint("SetTextI18n")
                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    project = ""
                                    textTo.text = "to project"
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

    private fun addRowToTable(code: String, fullPath: String) {
        val tableRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        val codeTextView = TextView(this).apply {
            text = code
            layoutParams = TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val pathTextView = TextView(this).apply {
            text = fullPath
            layoutParams = TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                3f
            )
        }

        // Add TextViews to the row
        tableRow.addView(codeTextView)
        tableRow.addView(pathTextView)

        // Add the row to the TableLayout
        locationTableLayout.addView(tableRow)
    }

    private fun visibilityManager () {
        when (searchMode) {
            "" -> {
                sampleLayout.visibility = View.GONE
                rangeLayout.visibility = View.GONE
                batchLayout.visibility = View.GONE
                searchButton.visibility = View.GONE
            }
            "sample" -> {
                rangeLayout.visibility = View.GONE
                batchLayout.visibility = View.GONE
                sampleLayout.visibility = View.VISIBLE
                if (project != "" && isIdEntryValid && isExtractEntryValid && isAliquotEntryValid) {
                    searchButton.visibility = View.VISIBLE
                } else {
                    searchButton.visibility = View.GONE
                }
            }
            "range" -> {
                batchLayout.visibility = View.GONE
                sampleLayout.visibility = View.VISIBLE
                rangeLayout.visibility = View.VISIBLE
                if (project != "" && isIdEntryValid && isExtractEntryValid && isAliquotEntryValid && isIdEntryRangeValid
                    && isExtractEntryRangeValid && isAliquotEntryRangeValid
                    && idEntry.text.toString().toInt() < idEntryRange.text.toString().toInt()
                    && ((extractEntry.text.toString() == "" && extractEntryRange.text.toString() == "") || (extractEntry.text.toString() != "" && extractEntryRange.text.toString() != ""))
                    && ((aliquotEntry.text.toString() == "" && aliquotEntryRange.text.toString() == "") || (aliquotEntry.text.toString() != "" && aliquotEntryRange.text.toString() != ""))
                    ) {
                    searchButton.visibility = View.VISIBLE
                } else {
                    searchButton.visibility = View.GONE
                }
            }
            "batch" -> {
                sampleLayout.visibility = View.GONE
                rangeLayout.visibility = View.GONE
                batchLayout.visibility = View.VISIBLE
                if (isIdEntryBatchValid) {
                    searchButton.visibility = View.VISIBLE
                } else {
                    searchButton.visibility = View.GONE
                }
            }
        }
    }

    // Function to easily display toasts.
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }
}