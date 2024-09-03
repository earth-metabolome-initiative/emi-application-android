package com.commons.emi

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bradysdk.api.printerconnection.PrinterUpdateListener
import com.bradysdk.api.printerdiscovery.DiscoveredPrinterInformation
import com.bradysdk.api.printerdiscovery.PrinterDiscoveryListener
import com.bradysdk.printengine.printinginterface.PrinterDiscoveryFactory
import com.google.android.material.navigation.NavigationView

abstract class BaseActivity : AppCompatActivity(), PrinterUpdateListener, PrinterDiscoveryListener {
    // Status icons elements
    private lateinit var connectionStatusIcon: ImageView
    private lateinit var printerStatusIcon: ImageView

    // drawer menu elements
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    // discover printers elements
    private lateinit var printerListView: ListView
    private lateinit var adapter: PrinterListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // status icons elements
        connectionStatusIcon = findViewById(R.id.connection_status_icon)
        printerStatusIcon = findViewById(R.id.printer_status_icon)

        // drawer menu elements
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // printer discovery element
        printerListView = findViewById(R.id.printerListView)

        setupNavigationDrawer()

        val toolbar: Toolbar = findViewById(R.id.toolbar) // Add this if you use Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_hamburger) // Ensure you have an icon
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        if (savedInstanceState == null) {
            setupContentFrame()
        }

        // Inflate the child activity's layout into the container
        val inflater = layoutInflater
        val container = findViewById<FrameLayout>(R.id.activity_content)
        inflater.inflate(getLayoutResourceId(), container, true)

        // Observe connection status changes
        DirectusTokenManager.isConnected.observe(this) { isConnected ->
            updateConnectionStatus(isConnected)
        }

        // Observe printer status changes
        PrinterManager.isConnected.observe(this) { isConnected ->
            updatePrinterStatus(isConnected)
        }

        // Create and display the available (detected) printers
        val printerList = ArrayList<DiscoveredPrinterInformation>()
        adapter = PrinterListAdapter(this, printerList)
        printerListView.adapter = adapter

        // Set an item click listener for the ListView
        printerListView.setOnItemClickListener { _, _, position, _ ->
            val selectedPrinter = adapter.getItem(position)
            // Pass it to connectToPrinter
            if (selectedPrinter != null) PrinterManager.connectToPrinter(
                this,
                selectedPrinter,
                this
            )
        }

        // Setup printer discovery and show in AlertDialog
        setupPrinterDiscovery(this)
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
        printerStatusIcon.setImageResource(
            if (isConnected) R.drawable.ic_green_printer
            else R.drawable.ic_red_printer
        )
        printerStatusIcon.visibility = View.VISIBLE
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_db -> {
                }
                R.id.nav_printer -> {
                    // Handle navigation to item 2
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

    protected abstract fun setupContentFrame()

    fun setupPrinterDiscovery(context: Context) {
        try {
            val printerDiscoveryListeners: MutableList<PrinterDiscoveryListener> = ArrayList()
            printerDiscoveryListeners.add(this)

            PrinterManager.printerDiscovery = PrinterDiscoveryFactory.getPrinterDiscovery(
                context.applicationContext,
                printerDiscoveryListeners
            )
            PrinterManager.printerDiscovery.startBlePrinterDiscovery()

            // Display printer discovery listeners in an AlertDialog
            showPrinterDiscoveryListenersDialog(printerDiscoveryListeners)

        } catch (ex: Exception) {
            println("Error: ${ex.message}")
        }
    }

    // Function to display PrinterDiscoveryListeners in an AlertDialog
    private fun showPrinterDiscoveryListenersDialog(printerDiscoveryListeners: List<PrinterDiscoveryListener>) {
        // Convert the listeners to a descriptive string list for display
        val items = printerDiscoveryListeners.map { listener ->
            // Customize this based on the properties of PrinterDiscoveryListener
            "Listener: ${listener.javaClass.simpleName}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Printer Discovery Listeners")
            .setItems(items, null)
            .setPositiveButton("OK", null)
            .show()
    }

    class PrinterListAdapter(
        context: Context,
        printerList: ArrayList<DiscoveredPrinterInformation>
    ) : ArrayAdapter<DiscoveredPrinterInformation>(context, 0, printerList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.printer_list_item, parent, false
            )

            val printerInfo = getItem(position)

            // Set the printer information in the view
            val printerNameTextView = view.findViewById<TextView>(R.id.printerNameTextView)
            printerNameTextView.text = printerInfo?.name

            return view
        }
    }
}