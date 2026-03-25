
package com.example.myfirst

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.content.IntentFilter

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "safety_status_channel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val LOCATION_PERMISSION_CODE = 101
    private val NOTIFICATION_PERMISSION_CODE = 102

    private lateinit var database: AppDatabase
    private lateinit var systemReceiver: SystemReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)
        systemReceiver = SystemReceiver()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()
        requestNotificationPermission()

        val rowCall = findViewById<LinearLayout>(R.id.rowCall)
        val rowSOS = findViewById<LinearLayout>(R.id.rowSOS)
        val rowLocation = findViewById<LinearLayout>(R.id.rowLocation)

        rowCall.setOnClickListener {
            showScheduleDatePicker()
        }

        rowCall.setOnLongClickListener {
            showHistoryDialog()
            true
        }

        rowSOS.setOnClickListener {
            showGpsProgressDialog()
        }

        rowLocation.setOnClickListener {
            showSOSConfirmDialog()
        }
    }

    // ================= DATABASE (CRUD) =================

    private fun insertCheckIn(time: String) {
        val newCheck = SafetyCheck(dateTime = time, status = "Pending")

        lifecycleScope.launch(Dispatchers.IO) {
            database.safetyDao().insert(newCheck)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Saved to Database", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showHistoryDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = database.safetyDao().getAllChecks()

            withContext(Dispatchers.Main) {
                if (list.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No history found", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val displayList = list.map { "${it.dateTime} - ${it.status}" }.toTypedArray()

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Safety History")
                    .setItems(displayList) { _, which ->
                        showUpdateDeleteOptions(list[which])
                    }
                    .setPositiveButton("Close", null)
                    .show()
            }
        }
    }

    private fun showUpdateDeleteOptions(check: SafetyCheck) {
        val options = arrayOf("Mark as Safe (Update)", "Delete Record")

        AlertDialog.Builder(this)
            .setTitle("Manage Record")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> updateStatus(check)
                    1 -> deleteRecord(check)
                }
            }
            .show()
    }

    private fun updateStatus(check: SafetyCheck) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.safetyDao().update(check.copy(status = "Safe"))
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Status Updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteRecord(check: SafetyCheck) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.safetyDao().delete(check)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= SCHEDULE =================

    private fun showScheduleDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            showScheduleTimePicker(year, month, day)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            .apply { setTitle("Select Safety Check Date") }
            .show()
    }

    private fun showScheduleTimePicker(year: Int, month: Int, day: Int) {
        val c = Calendar.getInstance()

        TimePickerDialog(this, { _, hour, minute ->
            val scheduledTime = "$day/${month + 1}/$year at $hour:$minute"

            insertCheckIn(scheduledTime)

            showStatusNotification(
                "Safety Check Scheduled",
                "Check-in set for $scheduledTime"
            )

            Toast.makeText(this, "Scheduled for: $scheduledTime", Toast.LENGTH_LONG).show()

        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)
            .apply { setTitle("Select Check-in Time") }
            .show()
    }

    // ================= LOCATION =================

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                currentLocation = it
            }
        }
    }

    private fun getAddressFromLocation(location: Location): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "Address not available"
            }
        } catch (e: Exception) {
            "Unable to get address"
        }
    }

    // ================= SOS =================

    private fun showSOSConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm SOS Alert")
            .setMessage("Notify emergency contacts of your location?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("SEND SOS") { _, _ ->

                getLastLocation()

                currentLocation?.let {
                    val message = """
                        SOS ALERT!
                        Latitude: ${it.latitude}
                        Longitude: ${it.longitude}
                        Address: ${getAddressFromLocation(it)}
                    """.trimIndent()

                    showStatusNotification("SOS DISPATCHED", message)
                    Toast.makeText(this, "SOS Sent!", Toast.LENGTH_LONG).show()
                } ?: Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ================= GPS SYNC =================

    private fun showGpsProgressDialog() {
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100

        val dialog = AlertDialog.Builder(this)
            .setTitle("Syncing GPS")
            .setView(progressBar)
            .setCancelable(false)
            .create()

        dialog.show()

        var progress = 0
        val handler = Handler(Looper.getMainLooper())

        Thread {
            while (progress < 100) {
                progress += 10
                handler.post { progressBar.progress = progress }
                Thread.sleep(200)
            }

            handler.post {
                dialog.dismiss()
                getLastLocation()

                currentLocation?.let {
                    showStatusNotification(
                        "Location Synced",
                        "Lat: ${it.latitude}, Lng: ${it.longitude}"
                    )
                }

                Toast.makeText(this, "GPS Sync Complete", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    // ================= NOTIFICATIONS =================

    private fun showStatusNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    // ================= POPUP =================

    private fun showPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(android.content.Intent.ACTION_BATTERY_LOW)
            addAction(android.content.Intent.ACTION_BATTERY_OKAY)
            addAction(android.content.Intent.ACTION_POWER_CONNECTED)
            addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(systemReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(systemReceiver)
    }
}