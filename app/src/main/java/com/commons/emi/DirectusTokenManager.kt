package com.commons.emi

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object DirectusTokenManager {
    private var baseUrl: String? = null
    private var username: String? = null
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
        DirectusTokenManager.baseUrl = baseUrl
        DirectusTokenManager.username = username
        DirectusTokenManager.password = password
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
}
