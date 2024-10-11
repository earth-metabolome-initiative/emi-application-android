package com.commons.emi

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

private var isPrinterConnected: Boolean = false
private var isDBConnected: Boolean = false

abstract class BaseActivity : AppCompatActivity() {
    // Status icons elements
    private lateinit var connectionStatusIcon: ImageView
    private lateinit var printerStatusIcon: ImageView

    // drawer menu elements
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var expListView: ExpandableListView
    private lateinit var listAdapter: CustomExpandableListAdapter
    private lateinit var listDataHeader: List<String>
    private lateinit var listDataChild: HashMap<String, List<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        // status icons elements
        connectionStatusIcon = findViewById(R.id.connection_status_icon)
        printerStatusIcon = findViewById(R.id.printer_status_icon)

        // drawer menu elements
        drawerLayout = findViewById(R.id.drawer_layout)
        expListView = findViewById(R.id.expListView)

        // Inflate the child activity's layout into the container
        val inflater = layoutInflater
        val container = findViewById<FrameLayout>(R.id.activity_content)
        inflater.inflate(getLayoutResourceId(), container, true)

        // Prepare the list data
        prepareListData()

        // Set up the adapter
        listAdapter = CustomExpandableListAdapter(this, listDataHeader, listDataChild)
        expListView.setAdapter(listAdapter)

        // Handle group (main menu) clicks
        expListView.setOnGroupClickListener { _, _, groupPosition, _ ->

            when (groupPosition) {
                0 -> drawerLayout.closeDrawers()
                3 -> openAbout()
                4 -> openHelp()
                5 -> openBug()
            }
            false
        }

        // Handle child (submenu) clicks
        expListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            when (groupPosition) {
                1 -> { // Printer settings
                    when (childPosition) {
                        0 -> connectDB()
                    }
                }
                2 -> { // Printer settings
                    when (childPosition) {
                        0 -> connectPrinter()
                    }
                }
            }
            false
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar) // Add this if you use Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_hamburger) // Ensure you have an icon
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Observe connection status changes
        DatabaseManager.isConnected.observe(this) { isConnected ->
            listAdapter.updateDBStatus(isConnected)
            updateConnectionStatus(isConnected)
        }

        // Observe printer status changes
        PrinterManager.isConnected.observe(this) { isConnected ->
            listAdapter.updatePrinterStatus(isConnected)
            updatePrinterStatus(isConnected)
        }
    }

    private fun connectDB() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("LAUNCHED", "yes")
        startActivity(intent)
    }

    // Method to restrict access if printer is not connected
    fun checkPrinterConnection() {
        if (!isPrinterConnected) {
            // Restrict access: Show a message or navigate back to a different activity
            showToast("Connect a printer to access this mode.")
            finish() // This will close the current activity
        }

    }

    // Method to restrict access if printer is not connected
    fun checkDBConnection() {
        if (isDBConnected) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

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
            if (PrinterManager.printerDetails.printerStatusMessage == "PrinterStatus_Initialized") {
                printerStatusIcon.setImageResource(R.drawable.ic_green_printer)
            } else if (PrinterManager.printerDetails.printerStatusMessage == "PrinterStatus_BatteryLow"){
                printerStatusIcon.setImageResource(R.drawable.ic_orange_printer)
            }
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

    // Populate menu data
    private fun prepareListData() {
        listDataHeader = listOf("", "Database settings", "Printer settings", "About us", "Help", "Report a bug")

        val directusSettings = listOf("Reconnect", "Status")
        val printerSettings = listOf("Connect a printer", "Status")
        val aboutUs = listOf<String>()
        val help = listOf<String>()
        val bug = listOf<String>()

        listDataChild = HashMap()
        listDataChild[listDataHeader[0]] = listOf() // Back doesn't have children
        listDataChild[listDataHeader[1]] = directusSettings
        listDataChild[listDataHeader[2]] = printerSettings
        listDataChild[listDataHeader[3]] = aboutUs
        listDataChild[listDataHeader[4]] = help
        listDataChild[listDataHeader[5]] = bug
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

    // function that permits to easily display temporary messages
    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }
}

@Suppress("NAME_SHADOWING")
class CustomExpandableListAdapter(
    private val context: Context,
    private val listDataHeader: List<String>,
    private val listDataChild: HashMap<String, List<String>>
) : BaseExpandableListAdapter() {

    // Update DB status dynamically
    fun updateDBStatus(isConnected: Boolean) {
        isDBConnected = isConnected
        notifyDataSetChanged() // Notify adapter to refresh UI
    }

    // Update Printer status dynamically
    fun updatePrinterStatus(isConnected: Boolean) {
        isPrinterConnected = isConnected
        notifyDataSetChanged() // Notify adapter to refresh UI
    }

    @SuppressLint("InflateParams")
    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertView: View?,
        parent: ViewGroup?
    ): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.group_item, null)
        }

        val groupTitle = convertView!!.findViewById<TextView>(R.id.group_title)
        val groupIcon = convertView.findViewById<ImageView>(R.id.group_icon)
        val indicatorIcon = convertView.findViewById<ImageView>(R.id.indicator_icon)

        groupTitle.text = listDataHeader[groupPosition]

        // Dynamically set icons for "Database settings" and "Printer settings"
        when (groupPosition) {
            0 -> groupIcon.setImageResource(R.drawable.ic_back_arrow)
            1 -> groupIcon.setImageResource(
                if (isDBConnected) R.drawable.ic_db_status_green else R.drawable.ic_db_status_red
            )
            2 -> groupIcon.setImageResource(
                if (isPrinterConnected) {
                    when (PrinterManager.printerDetails.printerStatusMessage) {
                        "PrinterStatus_Initialized" -> {
                            R.drawable.ic_green_printer
                        }

                        "PrinterStatus_BatteryLow" -> {
                            R.drawable.ic_orange_printer
                        }

                        else -> {
                            R.drawable.ic_red_printer
                        }
                    }
                } else {
                    R.drawable.ic_red_printer
                }
            )
            3 -> groupIcon.setImageResource(R.drawable.ic_info)
            4 -> groupIcon.setImageResource(R.drawable.ic_help)
            5 -> groupIcon.setImageResource(R.drawable.ic_bug)
        }

        // Handle indicator (expand/collapse) state
        if (getChildrenCount(groupPosition) > 0) {
            indicatorIcon.visibility = View.VISIBLE
            if (isExpanded) {
                indicatorIcon.setImageResource(R.drawable.ic_arrow_down)
            } else {
                indicatorIcon.setImageResource(R.drawable.ic_arrow_right)
            }
        } else {
            indicatorIcon.visibility = View.INVISIBLE
        }

        return convertView
    }

    @SuppressLint("InflateParams")
    override fun getChildView(
        groupPosition: Int, childPosition: Int, isLastChild: Boolean,
        convertView: View?, parent: ViewGroup?
    ): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null)
        }

        val textView = convertView!!.findViewById<TextView>(android.R.id.text1)
        val childText = getChild(groupPosition, childPosition) as String

        if (groupPosition == 2 && childPosition == 0) { // "Connect a printer"
            textView.text = childText
            if (isPrinterConnected) {
                textView.setTextColor(context.getColor(androidx.appcompat.R.color.material_grey_600)) // Set grey text color
                textView.isEnabled = false // Disable text view
            } else {
                textView.setTextColor(context.getColor(R.color.black)) // Reset to normal color
                textView.isEnabled = true // Enable text view
            }
        } else if (groupPosition == 2 && childPosition == 1) {
            textView.text = if (isPrinterConnected) "Printer status: ${PrinterManager.printerDetails.printerStatusMessage.replace("PrinterStatus_", "")}\nPrinter Model: ${PrinterManager.printerDetails.printerModel}" else "Printer status: Disconnected"
        } else if (groupPosition == 1 && childPosition == 0) { // "Connect to DB"
            textView.text = childText
            if (isDBConnected) {
                textView.setTextColor(context.getColor(androidx.appcompat.R.color.material_grey_600)) // Set grey text color
                textView.isEnabled = false // Disable text view
            } else {
                textView.setTextColor(context.getColor(R.color.black)) // Reset to normal color
                textView.isEnabled = true // Enable text view
            }
        } else if (groupPosition == 1 && childPosition == 1) {
            textView.text = if (isDBConnected) "${DatabaseManager.username} logged in" else "Status: Disconnected"
        } else {
            textView.text = childText
            textView.setTextColor(context.getColor(R.color.black)) // Default color for other items
            textView.isEnabled = true // Make sure other items are enabled
        }

        return convertView
    }

    // Other required methods...
    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return listDataChild[listDataHeader[groupPosition]]?.get(childPosition) ?: ""
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return listDataChild[listDataHeader[groupPosition]]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return listDataHeader[groupPosition]
    }

    override fun getGroupCount(): Int {
        return listDataHeader.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}