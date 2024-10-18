package com.commons.emi

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

// Trackers for db and printer labels
private var isPrinterConnected: Boolean = false
private var isDBConnected: Boolean = false

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var dbMenuItem: MenuItem
    private lateinit var printerMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)

        // Inflate the child activity's layout into the container
        val inflater = layoutInflater
        val container = findViewById<FrameLayout>(R.id.activity_content)
        inflater.inflate(getLayoutResourceId(), container, true)

        // Enable custom view in Action Bar
        supportActionBar?.apply {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_menu_db -> connectDB()
                R.id.nav_menu_db_info -> Log.d("clicked", "true")
                R.id.nav_menu_db_user -> Log.d("clicked", "true")
                R.id.nav_menu_printer -> connectPrinter()
                R.id.nav_menu_printer_info -> Log.d("clicked", "true")
                R.id.nav_menu_printer_model -> Log.d("clicked", "true")
                R.id.nav_menu_printer_ribbon -> Log.d("clicked", "true")
                R.id.nav_menu_about -> openAbout()
                R.id.nav_menu_help -> openHelp()
                R.id.nav_menu_bug -> openBug()
                else -> showToast("Unknown Error")
            }
            true
        }
    }

    private fun connectDB() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("LAUNCHED", "yes")
        startActivity(intent)
    }

    private fun connectPrinter() {
        if (!isPrinterConnected) {
            PermissionsManager.requestBluetoothPermission(this)
            PermissionsManager.enableBluetoothAndLocation(this)
            // Initialize PrinterManager and set the listener for printer discovery
            PrinterManager.initializePrinterDiscovery(this)
            PrinterManager.startPrinterDiscovery()
            PrinterManager.setOnPrintersDiscoveredListener {
                displayDiscoveredPrinters()
            }
            showToast("Starting printer discovery...")
        } else {
            showToast("A printer is already connected!")
        }
    }

    private fun displayDiscoveredPrinters() {
        // Get the list of discovered printers
        val discoveredPrinters = PrinterManager.getDiscoveredPrinters()
        val items = discoveredPrinters.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Connect a printer")
            .setNegativeButton("Cancel", fun(dialog: DialogInterface, _: Int) {
                dialog.dismiss()
            })
            .setItems(items) { dialog, which ->
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    // User pressed "Cancel"
                    dialog.dismiss()
                } else {
                    val selectedPrinter = discoveredPrinters[which]

                    // Show a toast with the selected printer's name
                    showToast("Connecting to ${selectedPrinter.name}...")
                    // Connect to the selected printer
                    PrinterManager.connectToPrinter(this, selectedPrinter)
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            }
            .create()
            .show()
    }

    private fun openAbout() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.earthmetabolome.org/")
        }
        startActivity(intent)
    }

    private fun openHelp() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/earth-metabolome-initiative/emi-application-android/blob/main/README.md")
        }
        startActivity(intent)
    }

    private fun openBug() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/earth-metabolome-initiative/emi-application-android/issues")
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)
        if (menu != null) {
            dbMenuItem = menu.findItem(R.id.dbIcon)
            printerMenuItem = menu.findItem(R.id.printerIcon)
            startWatching()
        }
        return true
    }

    private fun startWatching() {
        // Observe connection status changes
        DatabaseManager.isConnected.observe(this) { isConnected ->
            dbMenuItem.isVisible = true
            updateConnectionStatus(isConnected)
        }

        // Observe printer status changes
        PrinterManager.isConnected.observe(this) { isConnected ->
            printerMenuItem.isVisible = true
            updatePrinterStatus(isConnected)
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    fun setBreadcrumbs(breadcrumbs: List<Pair<String, Class<out Any>?>>) {
        // Inflate the breadcrumb layout
        val inflater = LayoutInflater.from(this)
        val breadcrumbsView = inflater.inflate(R.layout.breadcrumbs_layout, null)

        // Create a HorizontalScrollView to enable horizontal scrolling for breadcrumbs
        val horizontalScrollView = HorizontalScrollView(this).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT, // Make width wrap_content
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false // Optional, hides the scrollbar
        }

        // Create a LinearLayout to hold breadcrumbs inside the HorizontalScrollView
        val breadcrumbLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Dynamically create each breadcrumb
        breadcrumbs.forEachIndexed { index, (label, activityClass) ->
            // Create TextView for each breadcrumb
            val textView = TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 17f
                setPadding(8, 0, 8, 0)
                isClickable = activityClass != null // Only clickable if there's an activity to navigate to
                isFocusable = activityClass != null
            }

            // Set click listener for navigation, if activityClass is not null
            if (activityClass != null) {
                textView.setOnClickListener {
                    val intent = Intent(this, activityClass)
                    startActivity(intent)
                    finish()
                }
            }

            // Add the breadcrumb to the layout
            breadcrumbLayout.addView(textView)

            // Add a separator (except for the last item)
            if (index < breadcrumbs.size - 1) {
                val separator = TextView(this).apply {
                    text = " > "
                    setTextColor(Color.WHITE)
                    textSize = 15f
                }
                breadcrumbLayout.addView(separator)
            }
        }

        // Add the breadcrumbLayout to the HorizontalScrollView
        horizontalScrollView.addView(breadcrumbLayout)

        // Now, replace the breadcrumbLayout in breadcrumbsView with the horizontalScrollView
        val containerLayout = breadcrumbsView.findViewById<LinearLayout>(R.id.breadcrumbLayout)
        containerLayout.removeAllViews() // Clear any existing breadcrumbs
        containerLayout.addView(horizontalScrollView) // Add the scrollable breadcrumbs

        // Set the custom view in the Action Bar
        supportActionBar?.customView = breadcrumbsView
    }

    // Method to restrict access if printer is not connected
    fun checkPrinterConnection() {
        if (!isPrinterConnected) {
            // Restrict access: Show a message or navigate back to a different activity
            showToast("Connect a printer to access this mode.")
            finish() // This will close the current activity
        }

    }

    fun checkDBConnection() {
        if (isDBConnected) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        dbMenuItem.setIcon(if (isConnected) R.drawable.ic_db_status_green
        else R.drawable.ic_db_status_red)
        val menuItem = navView.menu.findItem(R.id.nav_menu_db_info)
        val userItem = navView.menu.findItem(R.id.nav_menu_db_user)
        if (isConnected) {
            menuItem.setIcon(R.drawable.ic_db_status_green)
            isDBConnected = true
            userItem.isVisible = true
            userItem.title = "user: ${DatabaseManager.username}"
            menuItem.title = "Connected"
        } else {
            menuItem.setIcon(R.drawable.ic_db_status_red)
            userItem.isVisible = false
            isDBConnected = false
            menuItem.title = "Disconnected"
        }
    }

    private fun updatePrinterStatus(isConnected: Boolean) {
        val menuItem = navView.menu.findItem(R.id.nav_menu_printer_info)
        val printItem = navView.menu.findItem(R.id.nav_menu_printer_model)
        val ribbonItem = navView.menu.findItem(R.id.nav_menu_printer_ribbon)
        if (isConnected) {
            when (PrinterManager.printerDetails.printerStatusMessage) {
                "PrinterStatus_Initialized" -> {
                    isPrinterConnected = true
                    if (PrinterManager.printerDetails.supplyRemainingPercentage > 5) {
                        menuItem.setIcon(R.drawable.ic_green_printer)
                        menuItem.title = "Status: Connected"
                        printItem.isVisible = true
                        printItem.title = "Model: ${PrinterManager.printerDetails.printerModel}"
                        ribbonItem.isVisible = true
                        ribbonItem.title = "Ribbon load: ${PrinterManager.printerDetails.supplyRemainingPercentage}%"
                        printerMenuItem.setIcon(R.drawable.ic_green_printer)
                    } else {
                        menuItem.setIcon(R.drawable.ic_orange_printer)
                        menuItem.title = "Status: Ribbon low"
                        printItem.isVisible = true
                        printItem.title = "Model: ${PrinterManager.printerDetails.printerModel}"
                        ribbonItem.isVisible = true
                        ribbonItem.title = "Ribbon load: ${PrinterManager.printerDetails.supplyRemainingPercentage}%"
                        printerMenuItem.setIcon(R.drawable.ic_orange_printer)
                    }

                }
                "PrinterStatus_BatteryLow" -> {
                    isPrinterConnected = true
                    menuItem.setIcon(R.drawable.ic_orange_printer)
                    menuItem.title = "Status: Battery low"
                    printItem.isVisible = true
                    printItem.title = "Model: ${PrinterManager.printerDetails.printerModel}"
                    ribbonItem.isVisible = true
                    ribbonItem.title = "Ribbon load: ${PrinterManager.printerDetails.supplyRemainingPercentage}%"
                    printerMenuItem.setIcon(R.drawable.ic_orange_printer)
                }
                else -> {
                    isPrinterConnected = true
                    menuItem.setIcon(R.drawable.ic_orange_printer)
                    menuItem.title = PrinterManager.printerDetails.printerStatusMessage.replace("PrinterStatus_", "")
                    printItem.isVisible = true
                    printItem.title = "Model: ${PrinterManager.printerDetails.printerModel}"
                    ribbonItem.isVisible = true
                    ribbonItem.title = "Ribbon load: ${PrinterManager.printerDetails.supplyRemainingPercentage}%"
                    printerMenuItem.setIcon(R.drawable.ic_orange_printer)
                }
            }
        } else {
            menuItem.setIcon(R.drawable.ic_red_printer)
            menuItem.title = "Disconnected"
            printItem.isVisible = false
            ribbonItem.isVisible = false
            isPrinterConnected = false
            printerMenuItem.setIcon(R.drawable.ic_red_printer)
            if (PrinterManager.printerDiscovery.lastConnectedPrinter != null) {
                PrinterManager.connectToPrinter(
                    this,
                    PrinterManager.printerDiscovery.lastConnectedPrinter
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)){
            navView.itemIconTintList = null
        }
        return true
    }

    @LayoutRes
    protected abstract fun getLayoutResourceId(): Int

    // Function to easily display toasts.
    private fun showToast(toast: String) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }
}