package com.commons.emi

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

object DatabaseManager {
    private var baseUrl: String? = null
    var username: String? = null
    private var password: String? = null
    private var accessToken: String? = null
    private var refreshJob: Job? = null
    private var connectionCheckJob: Job? = null

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    init {
        startConnectionCheck()
    }

    fun initialize(baseUrl: String, username: String, password: String) {
        DatabaseManager.baseUrl = baseUrl
        DatabaseManager.username = username
        DatabaseManager.password = password
        startTokenRefresh()
    }

    fun getAccessToken(): String? {
        return accessToken
    }

    fun getInstance(): String? {
        return baseUrl
    }

    private fun startTokenRefresh() {
        stopTokenRefresh() // Stop any existing job before starting a new one
        refreshJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                refreshToken()
                delay(TimeUnit.MINUTES.toMillis(10))
            }
        }
    }

    private fun stopTokenRefresh() {
        refreshJob?.cancel() // Cancel the refresh job if it's running
    }

    private fun refreshToken() {
        if (baseUrl == null || username == null || password == null) {
            Log.e("com.commons.emi.DirectusTokenManager", "Base URL, username, or password not initialized")
            return
        }

        try {
            val client = OkHttpClient()

            val jsonBody = JSONObject().apply {
                put("email", username)
                put("password", password)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("$baseUrl/auth/login")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val data = jsonResponse.getJSONObject("data")
                    accessToken = data.getString("access_token")
                    startConnectionCheck()
                    Log.d("com.commons.emi.DirectusTokenManager", "Access token refreshed: $accessToken")
                }
            } else {
                Log.e("com.commons.emi.DirectusTokenManager", "Failed to refresh token: ${response.code}")
                accessToken = null // Clear the token if refresh fails
                stopTokenRefresh() // Stop any ongoing refresh job on failure
            }
        } catch (e: IOException) {
            Log.e("com.commons.emi.DirectusTokenManager", "Error during token refresh: ${e.message}")
            accessToken = null // Clear the token on exception
            stopTokenRefresh() // Stop any ongoing refresh job on exception
        }
    }

    private fun startConnectionCheck() {
        connectionCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkConnection()
                delay(TimeUnit.SECONDS.toMillis(20)) // Check every 10 seconds
            }
        }
    }

    private fun checkConnection() {
        _isConnected.postValue(isServerReachable())
    }

    private fun isServerReachable(): Boolean {
        if (baseUrl == null || accessToken == null) {
            return false
        }

        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$baseUrl/items/Test_Connection") // Use a lightweight endpoint to test connection
                .addHeader("Authorization", "Bearer $accessToken")
                .head() // Use a HEAD request to minimize data transfer
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: IOException) {
            false
        }
    }

    suspend fun getContainerIdIfValid(container: String, isSampleContainer: Boolean): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            val collectionUrl = if (container.matches(Regex("^container_\\dx\\d_\\d{6}\$")) || container == "absent") {
                "${getInstance()}/items/Containers?filter[old_id][_eq]=$container&&limit=1"
            } else {
                "${getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"
            }

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
                        val isFinite = firstItem.optBoolean("is_finite")
                        val columns = firstItem.optInt("columns")
                        val rows = firstItem.optInt("rows")
                        if (isSampleContainer) {
                            result = if (isFinite) {
                                // Check if columns and rows are valid
                                val capacity = columns * rows
                                if (capacity == 1) {
                                    id
                                } else {
                                    -1
                                }
                            } else {
                                -1
                            }
                        } else {
                            if (isFinite) {
                                // Check if columns and rows are valid
                                val capacity = columns * rows
                                result = if (capacity > 1) {
                                    id
                                } else {
                                    -1
                                }
                            } else {
                                result = id
                            }
                        }
                    } else {
                        result = -3 // no data found
                    }
                } else {
                    result = -4 //response body null
                }
            } else {
                result = -5// non 200 response
            }
            result
        } catch (e: IOException) {
            -6 // internet or other errors
        }
    }

    suspend fun getContainerId(container: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            val collectionUrl = if (container.matches(Regex("^container_\\dx\\d_\\d{6}\$")) || container == "absent") {
                "${getInstance()}/items/Containers?filter[old_id][_eq]=$container&&limit=1"
            } else {
                "${getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"
            }

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
                    } else {
                        result = -3 // no data found
                    }
                } else {
                    result = -4 //response body null
                }
            } else {
                result = -5// non 200 response
            }
            result
        } catch (e: IOException) {
            -6 // internet or other errors
        }
    }

    suspend fun getContainerIdIfNew(container: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            val collectionUrl = if (container.matches(Regex("^container_\\dx\\d_\\d{6}\$")) || container == "absent") {
                "${getInstance()}/items/Containers?filter[old_id][_eq]=$container&&limit=1"
            } else {
                "${getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"
            }

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

                    result = if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        val id = firstItem.optInt("id")
                        val containerModel = firstItem.optString("container_model")
                        Log.d("rack", containerModel)
                        if (containerModel == "null") {
                            id
                        } else {
                            -1
                        }
                    } else {
                        -2 // no data found
                    }
                } else {
                    result = -3 //response body null
                }
            } else {
                result = -4// non 200 response
            }
            result
        } catch (e: IOException) {
            -5 // internet or other errors
        }
    }

    suspend fun getContainerModelId(containerId: Int): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            val collectionUrl = "${getInstance()}/items/Containers?filter[id][_eq]=$containerId&&limit=1"

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
                        val containerModel = firstItem.optInt("container_model")
                        result = containerModel
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

    suspend fun getBatchIdIfValid(batch: String, type: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            val batchType: Int = when (type) {
                "dried_samples" -> 7
                "extracts" -> 5
                "aliquots" -> 8
                else -> 0
            }

            val collectionUrl = "${getInstance()}/items/Batches?filter[batch_id][_eq]=$batch&&limit=1"

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
                        val typeBatch = firstItem.optInt("batch_type")
                        if (typeBatch == batchType) {
                            val id = firstItem.optInt("id")
                            id

                        } else {
                            result = -1 // batch is not of good type
                            result
                        }
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

    suspend fun checkContainerLoad(containerId: Int): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            // support old container names to avoid changing all labels
            val collectionUrl = "${getInstance()}/items/Containers?filter[id][_eq]=$containerId&&limit=1"

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
                        val isFinite = firstItem.optBoolean("is_finite")
                        if (isFinite) {
                            val columns = firstItem.optInt("columns")
                            val rows = firstItem.optInt("rows")
                            // Check if columns and rows are valid
                            val capacity = columns * rows
                            if (capacity == 1) {
                                result = -1
                                result

                            } else {
                                val load = checkLoad(containerId)
                                if (load == -1) {
                                    -2
                                } else {
                                    result = capacity - load
                                    result
                                }
                            }

                        } else {
                            result = -3 // container not finite
                            result
                        }
                    } else {
                        result = -4 // no data found
                        result
                    }
                } else {
                    result = -5 //response body null
                    result
                }
            } else {
                -6// non 200 response
            }
        } catch (e: IOException) {
            -7 // internet or other errors
        }
    }

    private suspend fun checkLoad(containerId: Int): Int = withContext(Dispatchers.IO) {
        try {
            // Prepare the URLs
            val collectionUrl = "${getInstance()}/items/Containers?filter[parent_container][_eq]=$containerId"

            // Initialize the HTTP client
            val client = OkHttpClient()

            // Prepare the requests
            val request = Request.Builder()
                .addHeader("Accept", "application/json")
                .url(collectionUrl)
                .build()

            // Execute the requests
            val response = client.newCall(request).execute()

            // Initialize total count
            val totalCount: Int

            // Parse the responses as JSON
            val responseBody = response.body

            // Sum the counts
            totalCount = countItems(responseBody)

            // Return the total count
            totalCount
        } catch (e: IOException) {
            -1
        }
    }

    // Function to count items in a JSON response (assuming it's a JSON array)
    private fun countItems(jsonResponse: ResponseBody?): Int{
        return if (jsonResponse != null) {
            val jsonString = jsonResponse.string() // Convert the ResponseBody to a JSON string
            val jsonObject = JSONObject(jsonString) // Parse the JSON string to JSONObject
            if (jsonObject.has("data")) {
                val dataArray = jsonObject.getJSONArray("data")
                dataArray.length() // Return the number of items in the "data" array
            } else {
                0 // Return 0 if the "data" key is not present
            }
        } else {
            0 // Return 0 if the response body is null
        }
    }

    suspend fun getExtractionDataPrimaryKey(extactId: Int): Int = withContext(Dispatchers.IO) {
        try {
            val id: Int

            val collectionUrl = "${getInstance()}/items/Extraction_Data?filter[sample_container][_eq]=$extactId&&limit=1"

            Log.d("DatabaseManager", collectionUrl)

            val client = OkHttpClient()
            val request = Request.Builder()
                .addHeader("Accept", "application/json")
                .url(collectionUrl)
                .build()

            val response = client.newCall(request).execute()
            Log.d("DatabaseManager", "error code: ${response.code}, error message: ${response.message}")
            if (response.code == HttpURLConnection.HTTP_OK) {
                val responseBody = response.body?.string()

                // Check if response body is not null and parse it
                if (responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val dataArray = jsonObject.getJSONArray("data")

                    if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        val idExtraction = firstItem.optInt("id")
                        id = idExtraction
                        id
                    } else {
                        id = -2 // no data found
                        id
                    }
                } else {
                    id = -3 //response body null
                    id
                }
            } else {
                -4// non 200 response
            }
        } catch (e: IOException) {
            -5 // internet or other errors
        }
    }

    suspend fun getContainerType(containerTypeId: Int): String = withContext(Dispatchers.IO) {
        try {
            val result: String

            val collectionUrl = "${getInstance()}/items/Container_Types?filter[id][_eq]=$containerTypeId&&limit=1"

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
                        val containerType = firstItem.optString("container_type")
                        result = containerType
                        result
                    } else {
                        result = "no_data" // no data found
                        result
                    }
                } else {
                    result = "null" //response body null
                    result
                }
            } else {
                "non_200"// non 200 response
            }
        } catch (e: IOException) {
            "internet" // internet or other errors
        }
    }

    suspend fun getUnit(unitId: Int): String = withContext(Dispatchers.IO) {
        try {
            val result: String

            val collectionUrl = "${getInstance()}/items/SI_Units?filter[id][_eq]=$unitId&&limit=1"

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
                        val symbol = firstItem.optString("symbol")
                        result = symbol
                        result
                    } else {
                        result = "no_data" // no data found
                        result
                    }
                } else {
                    result = "null" //response body null
                    result
                }
            } else {
                "non_200"// non 200 response
            }
        } catch (e: IOException) {
            "internet" // internet or other errors
        }
    }

    suspend fun getBrand(brandId: Int): String = withContext(Dispatchers.IO) {
        try {
            val result: String

            val collectionUrl = "${getInstance()}/items/Brands?filter[id][_eq]=$brandId&&limit=1"

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
                        val brand = firstItem.optString("brand")
                        result = brand
                        result
                    } else {
                        result = "no_data" // no data found
                        result
                    }
                } else {
                    result = "null" //response body null
                    result
                }
            } else {
                "non_200"// non 200 response
            }
        } catch (e: IOException) {
            "internet" // internet or other errors
        }
    }

    suspend fun checkContainerHierarchy(containerKey: Int, sampleKey: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val collectionUrl = "${getInstance()}/items/Container_Rules?filter[parent_container][_eq]=$containerKey&&filter[child_container][_eq]=$sampleKey&&limit=1"
            Log.d("DatabaseManager", collectionUrl)
            val client = OkHttpClient()
            val request = Request.Builder()
                .addHeader("Accept", "application/json")
                .url(collectionUrl)
                .build()

            val response = client.newCall(request).execute()
            if (response.code == HttpURLConnection.HTTP_OK) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody.toString())
                val dataArray = jsonObject.getJSONArray("data")
                Log.d("ContainerManager", "$dataArray")

                // Check if response body is not null and parse it
                dataArray.length() > 0

            } else {
                false
            }
        } catch (e: IOException) {
            false
        }
    }

    @SuppressLint("DefaultLocale")
    suspend fun getNewBatch(): String = withContext(Dispatchers.IO) {
        try {
            val result: String

            val collectionUrl = "${getInstance()}/items/Batches?sort[]=-batch_id&&limit=1"

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
                        val batchId = firstItem.optString("batch_id")
                        val lastBatchNumber = batchId.replace("batch_", "").toInt()
                        val firstBatchNumber = lastBatchNumber + 1
                        val paddedNumber = "batch_${String.format("%06d", firstBatchNumber)}"
                        paddedNumber
                    } else {
                        result = "no_data" // no data found
                        result
                    }
                } else {
                    result = "response_null" //response body null
                    result
                }
            } else {
                "non_200"// non 200 response
            }
        } catch (e: IOException) {
            "connection_error" // internet or other errors
        }
    }

    suspend fun getFullPath(container: String): String {
        return buildPath(container)
    }

    private suspend fun buildPath(container: String): String {
        val currentContainer = getLocation(container)

        // If the current container's location is "no_data", return immediately
        if (currentContainer == "no_data" || currentContainer == "null") return "no data found"

        // Get the parent container from the current container details
        val collectionUrl = "${getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"
        val client = OkHttpClient()
        val request = Request.Builder()
            .addHeader("Accept", "application/json")
            .url(collectionUrl)
            .build()

        val response = client.newCall(request).execute()
        if (response.code == HttpURLConnection.HTTP_OK) {
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val jsonObject = JSONObject(responseBody)
                val dataArray = jsonObject.getJSONArray("data")

                if (dataArray.length() > 0) {
                    val firstItem = dataArray.getJSONObject(0)
                    val parentId = firstItem.optInt("parent_container", -1)

                    // If there is no parent container, we are at the top-level container
                    if (parentId == -1) {
                        return ""
                    }

                    // Get the parent container string
                    val parentContainerString = getContainerString(parentId)

                    // Recursively build the path by calling buildPath for the parent container
                    return "$currentContainer > ${buildPath(parentContainerString)}"
                }
            }
        }
        return "Error retrieving full path" // error handling if something fails
    }

    private suspend fun getLocation(container: String): String = withContext(Dispatchers.IO) {
        try {
            val result: String

            val collectionUrl = "${getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"

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
                        val parentId = firstItem.optInt("parent_container")
                        val parent = getContainerString(parentId)
                        result = parent
                        result
                    } else {
                        result = "no_data" // no data found
                        result
                    }
                } else {
                    result = "null" //response body null
                    result
                }
            } else {
                "non_200"// non 200 response
            }
        } catch (e: IOException) {
            "internet" // internet or other errors
        }
    }

    private suspend fun getContainerString(containerId: Int): String = withContext(Dispatchers.IO) {
        try {
            val result: String

            val collectionUrl = "${getInstance()}/items/Containers?filter[id][_eq]=$containerId&&limit=1"

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
                        val container = firstItem.optString("container_id")
                        result = container
                        result
                    } else {
                        result = "no_data"
                        result
                    }
                } else {
                    result = "null"
                    result
                }
            } else {
                "non_200"
            }
        } catch (e: IOException) {
            "internet_error"
        }
    }
}
