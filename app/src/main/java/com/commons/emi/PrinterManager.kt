package com.commons.emi

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bradysdk.api.printerconnection.PrinterDetails
import com.bradysdk.api.printerconnection.PrinterProperties
import com.bradysdk.api.printerconnection.PrinterUpdateListener
import com.bradysdk.api.printerdiscovery.DiscoveredPrinterInformation
import com.bradysdk.api.printerdiscovery.PrinterDiscovery
import com.bradysdk.api.printerdiscovery.PrinterDiscoveryListener
import com.bradysdk.printengine.printinginterface.PrinterDiscoveryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object PrinterManager : AppCompatActivity(), PrinterUpdateListener, PrinterDiscoveryListener {
    lateinit var printerDetails: PrinterDetails
    lateinit var printerDiscovery: PrinterDiscovery
    private var connectionCheckJob: Job? = null

    // List to hold discovered printers
    private val discoveredPrinters = mutableListOf<DiscoveredPrinterInformation>()
    private var onPrintersDiscovered: (() -> Unit)? = null

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    // Initialize the printer discovery
    fun initializePrinterDiscovery(context: Context) {
        if (::printerDetails.isInitialized) {
            printerDetails.disconnectWithoutForget()
        }
        try {
            // Clear previous data and listeners
            discoveredPrinters.clear()
            onPrintersDiscovered = null
            printerDiscovery = PrinterDiscoveryFactory.getPrinterDiscovery(
                context.applicationContext,
                listOf(this)
            )
        } catch (ex: Exception) {
            Log.e("PrinterManager", "Error initializing printer discovery: ${ex.message}")
        }
    }

    // Start the BLE printer discovery process
    fun startPrinterDiscovery() {
        try {
            printerDiscovery.startBlePrinterDiscovery()
        } catch (ex: Exception) {
            Log.e("PrinterManager", "Error starting printer discovery: ${ex.message}")
        }
    }

    fun stopPrinterDiscovery() {
        try {
            printerDiscovery.stopPrinterDiscovery()
        } catch (ex: Exception) {
            Log.e("PrinterManager", "Error stopping printer discovery: ${ex.message}")
        }
    }

    // Method to set a callback for when printers are discovered
    fun setOnPrintersDiscoveredListener(callback: () -> Unit) {
        onPrintersDiscovered = callback
    }

    // Method to retrieve the list of discovered printers
    fun getDiscoveredPrinters(): List<DiscoveredPrinterInformation> {
        Log.d("PrinterManager", "updating the list")
        return discoveredPrinters
    }

    fun connectToPrinter(
        context: Context,
        printerSelected: DiscoveredPrinterInformation,
    ) {
        if (printerSelected != printerDiscovery.lastConnectedPrinter) {
            printerDiscovery.forgetLastConnectedPrinter()
        }
        val r = Runnable {
            try {

                this.printerDetails = printerDiscovery.connectToDiscoveredPrinter(
                    context,
                    printerSelected,
                    listOf(this)
                )!!
            } catch (_: NullPointerException) {

            } catch (_: Exception) {
            }
        }
        val connectThread = Thread(r)
        connectThread.start()
        startPrinterConnectionCheck()
    }

    private fun startPrinterConnectionCheck() {
        connectionCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkPrinterConnection()
                delay(TimeUnit.SECONDS.toMillis(10)) // Check every 10 seconds
            }
        }
    }

    private fun checkPrinterConnection() {
        _isConnected.postValue(isPrinterReachable())
    }

    private fun isPrinterReachable(): Boolean {
        return if (::printerDetails.isInitialized) {
            (printerDetails.printerStatusMessage == "PrinterStatus_Initialized"
                    || printerDetails.printerStatusMessage == "PrinterStatus_BatteryLow")
        } else {
            false
        }
    }

    override fun PrinterUpdate(p0: MutableList<PrinterProperties>?) {
        Log.d("PrinterManager", "Printer updated: $p0")
    }

    // This method is called when a printer is discovered
    override fun printerDiscovered(printer: DiscoveredPrinterInformation?) {
        printer?.let {
            discoveredPrinters.add(it)
            onPrintersDiscovered?.invoke() // Notify the UI
            Log.d("PrinterManager", "Printer discovered: ${it.name}")
        }
    }

    // This method is called when a printer is removed (if necessary to implement)
    override fun printerRemoved(printer: DiscoveredPrinterInformation?) {
        printer?.let {
            discoveredPrinters.remove(it)
            Log.d("PrinterManager", "Printer removed: ${it.name}")
            // You might want to notify the UI here if necessary
        }
    }

    override fun printerDiscoveryStarted() {
        Log.d("PrinterManager", "Printer discovery started")
    }

    override fun printerDiscoveryStopped() {
        Log.d("PrinterManager", "Printer discovery stopped")
    }
}