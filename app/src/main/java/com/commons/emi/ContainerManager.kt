package com.commons.emi

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection

object ContainerManager {
    suspend fun checkContainer(container: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int

            // support old container names to avoid changing all labels
            val collectionUrl = if (container.matches(Regex("^container_\\dx\\d_\\d{6}\$")) || container == "absent") {
                "${DirectusTokenManager.getInstance()}/items/Containers?filter[old_id][_eq]=$container&&limit=1"
            } else {
                "${DirectusTokenManager.getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"
            }
            Log.d("ContainerManager", "url: $collectionUrl")

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
                            val id = firstItem.optInt("id")
                            // Check if columns and rows are valid
                            val capacity = columns * rows
                            if (capacity == 1) {
                                result = -1
                                result

                            } else {
                                val load = checkLoad(id)
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

    private suspend fun checkLoad(id: Int): Int = withContext(Dispatchers.IO) {
        try {
            // Prepare the URLs
            val collectionUrlDried =
                "${DirectusTokenManager.getInstance()}/items/Dried_Samples_Data?filter[parent_container][_eq]=$id&&limit=-1"
            val collectionUrlExt =
                "${DirectusTokenManager.getInstance()}/items/Extraction_Data?filter[container_id][_eq]=$id&&limit=-1"
            val collectionUrlAl =
                "${DirectusTokenManager.getInstance()}/items/Aliquoting_Data?filter[container_id][_eq]=$id&&limit=-1"

            // Initialize the HTTP client
            val client = OkHttpClient()

            // Prepare the requests
            val requestDried = Request.Builder()
                .addHeader("Accept", "application/json")
                .url(collectionUrlDried)
                .build()
            val requestExt = Request.Builder()
                .addHeader("Accept", "application/json")
                .url(collectionUrlExt)
                .build()
            val requestAl = Request.Builder()
                .addHeader("Accept", "application/json")
                .url(collectionUrlAl)
                .build()

            // Execute the requests
            val responseDried = client.newCall(requestDried).execute()
            val responseExt = client.newCall(requestExt).execute()
            val responseAl = client.newCall(requestAl).execute()

            // Initialize total count
            val totalCount: Int

            // Parse the responses as JSON
            val responseBodyDried = responseDried.body
            val responseBodyExt = responseExt.body
            val responseBodyAl = responseAl.body

            // Count the items for each response
            val countDried = countItems(responseBodyDried)
            val countExt = countItems(responseBodyExt)
            val countAl = countItems(responseBodyAl)

            // Sum the counts
            totalCount = countDried + countExt + countAl
            //}

            // Return the total count
            totalCount
        } catch (e: IOException) {
            -1
        }
    }

    // Function to count items in a JSON response (assuming it's a JSON array)
    private fun countItems(jsonResponse: ResponseBody?): Int {
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

    suspend fun getPrimaryKey(container: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int
            val collectionUrl =
                "${DirectusTokenManager.getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"
            Log.d("ContainerManager", "url: $collectionUrl")

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

    suspend fun getContainerModel(container: String): Int = withContext(Dispatchers.IO) {
        try {
            val result: Int
            val collectionUrl =
                "${DirectusTokenManager.getInstance()}/items/Containers?filter[container_id][_eq]=$container&&limit=1"

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

    suspend fun checkContainerHierarchy(containerKey: Int, sampleKey: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val collectionUrl =
                "${DirectusTokenManager.getInstance()}/items/Container_Rules?filter[parent_container][_eq]=$containerKey&&filter[child_container][_eq]=$sampleKey&&limit=1"

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
}