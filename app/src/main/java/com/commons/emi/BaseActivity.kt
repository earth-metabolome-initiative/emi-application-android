package com.commons.emi

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

abstract class BaseActivity : AppCompatActivity() {
    // Status icons elements
    private lateinit var connectionStatusIcon: ImageView
    private lateinit var printerStatusIcon: ImageView

    // drawer menu elements
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // status icons elements
        connectionStatusIcon = findViewById(R.id.connection_status_icon)
        printerStatusIcon = findViewById(R.id.printer_status_icon)

        // drawer menu elements
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Inflate the child activity's layout into the container
        val inflater = layoutInflater
        val container = findViewById<FrameLayout>(R.id.activity_content)
        inflater.inflate(getLayoutResourceId(), container, true)

        setupNavigationDrawer()

        val toolbar: Toolbar = findViewById(R.id.toolbar) // Add this if you use Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_hamburger) // Ensure you have an icon
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Observe connection status changes
        DirectusTokenManager.isConnected.observe(this) { isConnected ->
            updateConnectionStatus(isConnected)
        }

        // Observe printer status changes
        PrinterManager.isConnected.observe(this) { isConnected ->
            updatePrinterStatus(isConnected)
        }
    }

    @LayoutRes
    protected abstract fun getLayoutResourceId(): Int

    private fun updateConnectionStatus(isConnected: Boolean) {
        connectionStatusIcon.setImageResource(
            if (isConnected) R.drawable.ic_db_status_green
            else R.drawable.ic_db_status_red
        )
        connectionStatusIcon.visibility = View.VISIBLE
    }

    private fun updatePrinterStatus(isConnected: Boolean) {
        if (isConnected) {
            printerStatusIcon.setImageResource(R.drawable.ic_green_printer)
        } else {
            printerStatusIcon.setImageResource(R.drawable.ic_red_printer)
            if (PrinterManager.printerDiscovery.lastConnectedPrinter != null) {
                PrinterManager.connectToPrinter(
                    this,
                    PrinterManager.printerDiscovery.lastConnectedPrinter
                )
            }
        }
        printerStatusIcon.visibility = View.VISIBLE
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_db -> {
                }
                R.id.nav_printer -> {
                    PermissionsManager.requestBluetoothPermission(this)
                    PermissionsManager.enableBluetoothAndLocation(this)
                    // Initialize PrinterManager and set the listener for printer discovery
                    PrinterManager.initializePrinterDiscovery(this)
                    PrinterManager.startPrinterDiscovery()
                    PrinterManager.setOnPrintersDiscoveredListener {
                        displayDiscoveredPrinters()
                    }
                    showToast("Starting printer discovery...")
                }
                R.id.nav_about -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://www.earthmetabolome.org/")
                    }
                    startActivity(intent)
                }
                R.id.nav_help -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/earth-metabolome-initiative/emi-application-android/blob/main/README.md")
                    }
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private var alertDialog: Unit? = null
    private fun displayDiscoveredPrinters() {
        // Get the list of discovered printers
        val discoveredPrinters = PrinterManager.getDiscoveredPrinters()
        val items = discoveredPrinters.map { it.name }.toTypedArray()

        alertDialog = null

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Connect a printer")
            .setNegativeButton("Cancel", fun(dialog: DialogInterface, _: Int) {
                PrinterManager.stopPrinterDiscovery()
                dialog.dismiss()
            })
            .setItems(items) { dialog, which ->
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    // User pressed "Cancel"
                    PrinterManager.stopPrinterDiscovery()
                    dialog.dismiss()
                } else {
                    val selectedPrinter = discoveredPrinters[which]

                    // Stop discovery first to prevent new printers from being added after the dialog is shown
                    PrinterManager.stopPrinterDiscovery()
                    // Show a toast with the selected printer's name
                    showToast("Connecting to ${selectedPrinter.name}...")
                    // Connect to the selected printer
                    PrinterManager.connectToPrinter(this, selectedPrinter)
                    // Dismiss the dialog
                    dialog.dismiss()
                    alertDialog = null
                }
            }
            .create()
            .show()
    }



    // function that permits to easily display temporary messages
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }
}