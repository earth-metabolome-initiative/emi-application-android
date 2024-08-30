package com.commons.emi

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bradysdk.api.printerconnection.PrinterDetails
import com.bradysdk.api.printerconnection.PrinterUpdateListener
import com.bradysdk.api.printerdiscovery.DiscoveredPrinterInformation
import com.bradysdk.api.printerdiscovery.PrinterDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object PrinterManager : AppCompatActivity() {
    lateinit var printerDiscovery: PrinterDiscovery
    lateinit var printerDetails: PrinterDetails
    private var value: Int = -1
    private var connectionCheckJob: Job? = null

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> get() = _isConnected

    private fun disconnectPreviousPrinter() {
        val lastConnectedPrinter = printerDiscovery.lastConnectedPrinter
        if (lastConnectedPrinter != null) {
            printerDiscovery.forgetLastConnectedPrinter()
        }
    }


    fun connectToPrinter(
        context: Context,
        printerSelected: DiscoveredPrinterInformation,
        pul: PrinterUpdateListener,
    ): Int {

        disconnectPreviousPrinter()

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

    fun sendPrintDiscovery() {}

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
        if (::printerDetails.isInitialized) {
            if (printerDetails.printerStatusMessage == "PrinterStatus_Initialized"
                || printerDetails.printerStatusMessage == "PrinterStatus_BatteryLow"
            ) {
                return true
            }
        } else {
            // implement an automatic reconnection procedure
            return false
        }
        return false
    }
}