package com.commons.emi

import android.os.Bundle
import android.graphics.Bitmap
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import com.bradysdk.api.printerconnection.CutOption
import com.bradysdk.api.printerconnection.PrintingOptions
import com.bradysdk.api.printerconnection.PrintingStatus
import com.bradysdk.api.printerconnection.PrinterDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.json.JSONArray

internal fun isTubeContainerModel(model: JSONObject): Boolean =
    model.optBoolean("is_sample_container") &&
        model.optDouble("volume") > 0.0 &&
        model.has("columns") && model.has("rows") &&
        capacityFromDimensions(model.optInt("columns"), model.optInt("rows")) == 1

internal fun isRelabelableSampleContainer(container: JSONObject, hasFieldSample: Boolean): Boolean =
    container.has("container_model") && if (container.isNull("container_model")) {
        hasFieldSample
    } else {
        DatabaseManager.capacityFromContainerModel(container) == 1
    }

internal suspend fun resolveTubeSampleContainerId(
    code: String, baseUrl: HttpUrl, token: String?, client: OkHttpClient
): Int = withContext(Dispatchers.IO) {
    val key = if (code.matches(Regex("^container_\\dx\\d_\\d{6}$"))) "old_id" else "container_id"
    val containersUrl = baseUrl.newBuilder().addPathSegments("items/Containers")
        .addQueryParameter("fields", "id,container_model.columns,container_model.rows")
        .addQueryParameter("filter[$key][_eq]", code)
        .addQueryParameter("limit", "2").build()
    val containers = fetchTubeLabelData(containersUrl, token, client)
    if (containers.length() == 0) error("No container record found for $code")
    if (containers.length() != 1) error("More than one container record matches $code")
    val container = containers.getJSONObject(0)
    if (!container.has("container_model")) error("Cannot read the container's current model")

    val hasFieldSample = if (container.isNull("container_model")) {
        val fieldUrl = baseUrl.newBuilder().addPathSegments("items/Field_Data")
            .addQueryParameter("fields", "id")
            .addQueryParameter("filter[sample_id][_eq]", code)
            .addQueryParameter("limit", "1").build()
        fetchTubeLabelData(fieldUrl, token, client).length() > 0
    } else false

    if (!isRelabelableSampleContainer(container, hasFieldSample)) {
        error(if (container.isNull("container_model")) {
            "No matching Field Data sample found for $code"
        } else {
            "Container is already assigned to a non-sample model"
        })
    }
    container.getInt("id").takeIf { it > 0 } ?: error("Invalid container ID")
}

private fun fetchTubeLabelData(url: HttpUrl, token: String?, client: OkHttpClient): JSONArray {
    val request = Request.Builder().url(url).apply {
        token?.let { header("Authorization", "Bearer $it") }
    }.build()
    return client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) error("Directus lookup failed (${response.code})")
        JSONObject(response.body?.string() ?: error("Empty Directus response")).getJSONArray("data")
    }
}

class LogisticTubeLabelActivity : BaseActivity() {
    private val client = OkHttpClient()
    private lateinit var modelSpinner: Spinner
    private lateinit var scanButton: Button
    private lateinit var sizeSpinner: Spinner
    private lateinit var printButton: Button
    private lateinit var scannedLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var scanLayout: View
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: ImageButton
    private var models: List<Pair<Int, String>> = emptyList()
    private var selectedModelId: Int? = null
    private var selectedSize: TubeLabelSize? = null
    private var scannedCode: String? = null
    private var scanning = false
    private var busy = false

    override fun getLayoutResourceId() = R.layout.activity_logistic_tube_label

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Bag to tube labels"
        setBreadcrumbs(listOf(
            Pair("Login", LoginActivity::class.java),
            Pair("Home", HomeActivity::class.java),
            Pair("Logistic", HomeLogisticActivity::class.java),
            Pair("Bag to tube labels", null)
        ))

        modelSpinner = findViewById(R.id.tubeModelSpinner)
        sizeSpinner = findViewById(R.id.tubeLabelSizeSpinner)
        scanButton = findViewById(R.id.tubeScanButton)
        printButton = findViewById(R.id.tubePrintButton)
        scannedLabel = findViewById(R.id.tubeScannedLabel)
        statusLabel = findViewById(R.id.tubeStatusLabel)
        scanLayout = findViewById(R.id.scanLayout)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)

        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedModelId = models.getOrNull(position - 1)?.first
                updateButtons()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedModelId = null
                updateButtons()
            }
        }
        val sizes = TubeLabelSize.entries
        sizeSpinner.adapter = ArrayAdapter(
            this, R.layout.spinner_list,
            listOf("Choose a label size") + sizes.map { it.displayName }
        ).apply { setDropDownViewResource(R.layout.spinner_list) }
        sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSize = sizes.getOrNull(position - 1)
                updateButtons()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSize = null
                updateButtons()
            }
        }
        scanButton.setOnClickListener {
            scanning = true
            scanLayout.visibility = View.VISIBLE
            findViewById<TextView>(R.id.scanStatus).text = "Scan the sample bag QR code"
            ScanManager.initialize(this, previewView, flashlightButton, callback = scan@{ value ->
                if (!scanning) return@scan
                stopScanner()
                if (value.isBlank()) {
                    statusLabel.text = "Empty QR code. Scan again."
                } else {
                    scannedCode = value
                    scannedLabel.text = value
                    statusLabel.text = "Confirm the tube type, then print the sticker."
                }
                updateButtons()
            })
        }
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener { stopScanner() }
        printButton.setOnClickListener { attributeAndPrint() }
        updateButtons()
        loadModels()
    }

    override fun onStop() {
        if (scanning) stopScanner()
        super.onStop()
    }

    private fun stopScanner() {
        if (!scanning) return
        scanning = false
        scanLayout.visibility = View.GONE
        ScanManager.stopScanning()
    }

    private fun updateButtons() {
        scanButton.isEnabled = !busy && !scanning && selectedModelId != null
        printButton.isEnabled = !busy && !scanning && selectedModelId != null &&
            scannedCode != null && selectedSize != null
        modelSpinner.isEnabled = !busy && !scanning
        sizeSpinner.isEnabled = !busy && !scanning
    }

    private fun loadModels() {
        val baseUrl = DatabaseManager.getInstance()
        val token = DatabaseManager.getAccessToken()
        if (baseUrl == null || token == null) {
            statusLabel.text = "Sign in to Directus to load tube types."
            return
        }
        lifecycleScope.launch {
            try {
                val options = withContext(Dispatchers.IO) {
                    val url = "$baseUrl/items/Container_Models?fields=id,is_sample_container,volume,container_type,volume_unit,brand,columns,rows&limit=-1"
                    val request = Request.Builder().url(url)
                        .header("Authorization", "Bearer $token").build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("Could not load container types (${response.code})")
                        val data = JSONObject(response.body?.string() ?: error("Empty container types response"))
                            .getJSONArray("data")
                        buildList {
                            for (i in 0 until data.length()) {
                                val item = data.getJSONObject(i)
                                if (!isTubeContainerModel(item)) continue
                                val id = item.getInt("id")
                                val type = DatabaseManager.getContainerType(item.optInt("container_type"))
                                val unit = DatabaseManager.getUnit(item.optInt("volume_unit"))
                                val brand = DatabaseManager.getBrand(item.optInt("brand"))
                                add(id to "$type ${item.optDouble("volume")} $unit $brand (#$id)")
                            }
                        }
                    }
                }
                models = options
                val labels = listOf("Choose a tube container type") + options.map { it.second }
                modelSpinner.adapter = ArrayAdapter(this@LogisticTubeLabelActivity, R.layout.spinner_list, labels).apply {
                    setDropDownViewResource(R.layout.spinner_list)
                }
                statusLabel.text = if (options.isEmpty()) "No single-sample container types available." else "Select the new tube type."
            } catch (e: Exception) {
                statusLabel.text = "Could not load container types: ${e.message}"
            }
        }
    }

    private fun attributeAndPrint() {
        val code = scannedCode ?: return
        val modelId = selectedModelId ?: return
        val size = selectedSize ?: return
        if (busy) return
        if (PrinterManager.isConnected.value != true) {
            statusLabel.text = "Connect a Brady printer before printing."
            return
        }
        val printer = PrinterManager.printerDetails
        if (printer.printerModel != "M211" && printer.printerModel != "M511") {
            statusLabel.text = "Unsupported printer: ${printer.printerModel}"
            return
        }
        val mediaError = tubeLabelSupplyError(size, printer.supplyWidth, printer.isSupplyPreSized == true)
        if (mediaError != null) {
            statusLabel.text = mediaError
            return
        }
        val rotated = rotateTubeLabelOnTape(size, printer.supplyWidth)
        val layoutLengthInches = if (rotated) minOf(size.lengthInches, printer.supplyWidth) else size.lengthInches
        val compact = rotated && layoutLengthInches < size.lengthInches
        busy = true
        updateButtons()
        statusLabel.text = "Checking sample and updating its tube type..."
        lifecycleScope.launch {
            var attributed = false
            try {
                val id = resolveSampleContainerId(code)
                val label = try {
                    renderTubeLabel(code, size, printer.dotsPerInch, rotated, layoutLengthInches)
                } catch (e: Exception) {
                    statusLabel.text = "Cannot print this label: ${e.message}"
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    val token = DatabaseManager.getAccessToken() ?: error("Not signed in to Directus")
                    val body = JSONObject().put("container_model", modelId).toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url("${DatabaseManager.getInstance()}/items/Containers/$id")
                        .header("Authorization", "Bearer $token").patch(body).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("Could not attribute tube type (${response.code})")
                    }
                }
                attributed = true
                statusLabel.text = when {
                    compact -> "Tube type saved. Printing compact Big sticker across the tape..."
                    rotated -> "Tube type saved. Printing across the tape to save material..."
                    else -> "Tube type saved. Printing sticker..."
                }
                val result = printSticker(label, size, printer, rotated, layoutLengthInches)
                if (result == PrintingStatus.PrintingSucceeded) {
                    statusLabel.text = "Sticker printed for $code${if (compact) " (compact Big across the tape)" else if (rotated) " across the tape" else ""}. Apply it to the tube."
                    scannedCode = null
                    scannedLabel.text = "No sample scanned"
                } else {
                    statusLabel.text = "Tube type saved, but printing was not confirmed ($result). Check the printer before retrying."
                }
            } catch (e: Exception) {
                statusLabel.text = if (attributed) {
                    "Tube type saved, but printing failed: ${e.message}. Check the printer before retrying."
                } else {
                    "Could not update the tube type: ${e.message}. Try again."
                }
            } finally {
                busy = false
                updateButtons()
            }
        }
    }

    private suspend fun resolveSampleContainerId(code: String): Int {
        val baseUrl = DatabaseManager.getInstance()?.toHttpUrl() ?: error("Sign in to Directus")
        val token = DatabaseManager.getAccessToken() ?: error("Sign in to Directus")
        return resolveTubeSampleContainerId(code, baseUrl, token, client)
    }

    private fun printSticker(
        label: Bitmap, size: TubeLabelSize, printer: PrinterDetails, rotated: Boolean, layoutLengthInches: Double
    ): PrintingStatus = printer.print(this, label,
        if (rotated) layoutLengthInches else size.widthInches,
        if (rotated) size.widthInches else layoutLengthInches,
        PrintingOptions().apply {
            cutOption = CutOption.EndOfLabel
            numberOfCopies = 1
        }, if (printer.printerModel == "M211") true else null)
}
