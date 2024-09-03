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
    lateinit var printerDiscovery: PrinterDiscovery
    lateinit var printerDetails: PrinterDetails
    private var value: Int = -1
    private var connectionCheckJob: Job? = null

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    fun discoverPrinters(context: Context) {
        val printerDiscoveryListeners: MutableList<PrinterDiscoveryListener> = ArrayList()
        printerDiscoveryListeners.add(this)
        printerDiscovery = PrinterDiscoveryFactory.getPrinterDiscovery(
            context.applicationContext,
            printerDiscoveryListeners
        )
        printerDiscovery.startBlePrinterDiscovery()
        discoverPrinters(context)
    }


    fun connectToPrinter(
        context: Context,
        printerSelected: DiscoveredPrinterInformation,
        pul: PrinterUpdateListener,
    ): Int {
        startPrinterConnectionCheck()
        val r = Runnable {
            try {

                this.printerDetails = printerDiscovery.connectToDiscoveredPrinter(
                    context,
                    printerSelected,
                    listOf(pul)
                )!!
            } catch (_: NullPointerException) {

            } catch (_: Exception) {
            }
        }
        val connectThread = Thread(r)
        connectThread.start()
        return value
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
            // implement an automatic reconnection procedure
            false
        }
    }

    override fun PrinterUpdate(p0: MutableList<PrinterProperties>?) {
        TODO("Not yet implemented")
    }

    override fun printerDiscovered(p0: DiscoveredPrinterInformation?) {
        TODO("Not yet implemented")
    }

    override fun printerRemoved(p0: DiscoveredPrinterInformation?) {
        TODO("Not yet implemented")
    }

    override fun printerDiscoveryStarted() {
        Log.d("PrinterManager", "printer discovery started")
    }

    override fun printerDiscoveryStopped() {
        TODO("Not yet implemented")
    }
}