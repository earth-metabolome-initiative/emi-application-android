package com.commons.emi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bradysdk.api.printerconnection.PrinterProperties
import com.bradysdk.api.printerconnection.PrinterUpdateListener
import com.bradysdk.api.printerdiscovery.DiscoveredPrinterInformation
import com.bradysdk.api.printerdiscovery.PrinterDiscoveryListener
import com.bradysdk.printengine.printinginterface.PrinterDiscoveryFactory

class PrinterConnectActivity : BaseActivity(), PrinterUpdateListener, PrinterDiscoveryListener {

    private lateinit var printerListView: ListView
    private lateinit var adapter: PrinterListAdapter

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_printer_connect // Replace with your actual login layout file
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_printer_connect)

        title = "Printer management screen"

        // Initialize the ListView
        printerListView = findViewById(R.id.printerListView)

        // Creates and displays the available (detected) printers
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
            setPrinterDetails()

        }

        setupPrinterDiscovery(this)
    }

    // Searches for available printers
    private fun setupPrinterDiscovery(context: Context) {
        try {
            val printerDiscoveryListeners: MutableList<PrinterDiscoveryListener> = ArrayList()
            printerDiscoveryListeners.add(this)
            PrinterManager.printerDiscovery = PrinterDiscoveryFactory.getPrinterDiscovery(
                context.applicationContext,
                printerDiscoveryListeners
            )
            PrinterManager.sendPrintDiscovery()
            PrinterManager.printerDiscovery.startBlePrinterDiscovery()
            showToast("Select a printer...")
        } catch (ex: Exception) {
            println("Error: ${ex.message}")
        }
    }

    private val isPrinterConnected = "yes"

    // Launches activity to wait printer connection before redirecting to homepage.
    // Permits to be sure that the printer is successfully connected.
    private fun setPrinterDetails() {
        showToast("connecting...")
        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")

        val intent = Intent(this, LabWeighingActivity::class.java)
        intent.putExtra("ACCESS_TOKEN", accessToken)
        intent.putExtra("USERNAME", username)
        intent.putExtra("PASSWORD", password)
        intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
        startActivity(intent)
    }

    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }

    override fun printerDiscovered(p0: DiscoveredPrinterInformation?) {
        p0?.let {
            // Check if a printer with the same name already exists in the list
            adapter.clear()
            // Add the discovered printer to the list
            adapter.add(it)
            adapter.notifyDataSetChanged()
        }
    }

    fun printerRemoved(p0: DiscoveredPrinterInformation?) {
    }

    override fun printerDiscoveryStarted() {
    }

    override fun printerDiscoveryStopped() {
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

    override fun PrinterUpdate(p0: MutableList<PrinterProperties>?) {
    }
}