package com.commons.emi

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class LoginActivity : BaseActivity() {

    // Initialize UI elements
    private lateinit var textViewBaseUrl: TextView
    private lateinit var editTextBaseUrl:EditText
    private lateinit var welcomeMessage: TextView
    private lateinit var loginMessage: TextView
    private lateinit var textViewUsername: TextView
    private lateinit var editTextUsername: EditText
    private lateinit var textViewPassword: TextView
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_login // Replace with your actual login layout file
    }

    @SuppressLint("SetTextI18n")
    // Function that is launched when activity is called
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the link with the corresponding xml
        //setContentView(R.layout.activity_login)

        title = "Directus connection screen"

        getLatestReleaseVersion()
    }

    @SuppressLint("SetTextI18n")
    private fun getLatestReleaseVersion() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/digital-botanical-gardens-initiative/DBGI_tracking_android/releases/latest")
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection()
                } as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    inputStream.close()

                    // Parse JSON response to get the tag_name
                    val jsonResponse = JSONObject(response.toString())
                    val tagName = jsonResponse.getString("tag_name")

                    // Compare github tag to actual application version to detect if there is a new version available
                    val version = tagName.replace("v", "").toFloat()
                    val currentVersion = BuildConfig.VERSION_NAME.toFloat()
                    if (version <= currentVersion){
                        withContext(Dispatchers.Main) {
                            performConnection()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            goToNewRelease()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Error: $responseCode")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    welcomeMessage = findViewById(R.id.welcomeMessage)
                    welcomeMessage.visibility = View.VISIBLE
                    welcomeMessage.text = "No internet connection. Please connect to a network and reload the application."
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun performConnection() {
        // Retrieve UI elements from xml to perform action on them
        textViewBaseUrl = findViewById(R.id.baseUrlTextView)
        editTextBaseUrl = findViewById(R.id.baseUrlEditText)
        welcomeMessage = findViewById(R.id.welcomeMessage)
        loginMessage = findViewById(R.id.LoginMessage)
        textViewUsername = findViewById(R.id.usernameTextView)
        editTextUsername = findViewById(R.id.usernameEditText)
        textViewPassword = findViewById(R.id.passwordTextView)
        editTextPassword = findViewById(R.id.passwordEditText)
        buttonLogin = findViewById(R.id.loginButton)

        // Change welcome text and login button to ask user to download the new version
        textViewBaseUrl.visibility = View.VISIBLE
        editTextBaseUrl.visibility = View.VISIBLE
        welcomeMessage.visibility = View.VISIBLE
        loginMessage.visibility = View.VISIBLE
        textViewUsername.visibility = View.VISIBLE
        editTextUsername.visibility = View.VISIBLE
        textViewPassword.visibility = View.VISIBLE
        editTextPassword.visibility = View.VISIBLE
        buttonLogin.visibility = View.VISIBLE

        // Define actions that are performed when user click on login button
        buttonLogin.setOnClickListener {

            // display a message to inform user that the connection is in progress
            showToast("Connecting...")

            // Retrieve username and password entered by the user
            val baseUrl = editTextBaseUrl.text.toString()
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()

            // Perform login in a coroutine to keep it asynchronous
            CoroutineScope(Dispatchers.Main).launch {
                val accessToken = withContext(Dispatchers.IO) {
                    DirectusTokenManager.initialize(baseUrl, username, password)
                    delay(2000)
                    DirectusTokenManager.getAccessToken()
                }

                if (accessToken != null) {
                    showToast("Connected!")
                    // Successful login, proceed to the next activity
                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Login failed, show an error message
                    showToast("Login failed. Please check your credentials.")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun goToNewRelease() {
        // Retrieve UI elements from xml to perform action on them
        welcomeMessage = findViewById(R.id.welcomeMessage)
        textViewUsername = findViewById(R.id.usernameTextView)
        editTextUsername = findViewById(R.id.usernameEditText)
        textViewPassword = findViewById(R.id.passwordTextView)
        editTextPassword = findViewById(R.id.passwordEditText)
        buttonLogin = findViewById(R.id.loginButton)

        // Change welcome text and login button to ask user to download the new version
        welcomeMessage.visibility = View.VISIBLE
        welcomeMessage.text = "A new version of the application is available, please click on the button below to download it."
        buttonLogin.visibility = View.VISIBLE
        buttonLogin.text = "Download latest version"

        // Define actions that are performed when user click on login button
        buttonLogin.setOnClickListener {

            val url = "https://github.com/digital-botanical-gardens-initiative/DBGI_tracking_android/releases/latest"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }
    }

    // function that permits to easily display temporary messages
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }

}