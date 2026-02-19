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
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import java.util.*

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "safety_status_channel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val LOCATION_PERMISSION_CODE = 101
    private val NOTIFICATION_PERMISSION_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()
        requestNotificationPermission()

        val btnSchedule = findViewById<ImageButton>(R.id.imageButton7)
        val btnSync = findViewById<ImageButton>(R.id.imageButton4)
        val btnSOS = findViewById<ImageButton>(R.id.imageButton2)

        registerForContextMenu(btnSchedule)
        registerForContextMenu(btnSync)
        registerForContextMenu(btnSOS)

        btnSOS.setOnClickListener { showSOSConfirmDialog() }
        btnSync.setOnClickListener { showGpsProgressDialog() }
        btnSchedule.setOnClickListener { showScheduleDatePicker() }

        btnSchedule.setOnLongClickListener { showPopup(it); true }
        btnSync.setOnLongClickListener { showPopup(it); true }
        btnSOS.setOnLongClickListener { showPopup(it); true }
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                }
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm SOS Alert")
        builder.setMessage("Are you sure you want to notify all emergency contacts of your current location?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("SEND SOS") { _, _ ->

            getLastLocation()

            if (currentLocation != null) {

                val lat = currentLocation!!.latitude
                val lng = currentLocation!!.longitude
                val address = getAddressFromLocation(currentLocation!!)

                val message = """
                    SOS ALERT!
                    
                    Latitude: $lat
                    Longitude: $lng
                    Address: $address
                """.trimIndent()

                showStatusNotification("SOS DISPATCHED", message)
                Toast.makeText(this, "SOS Sent with Location!", Toast.LENGTH_LONG).show()

            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // ================= GPS SYNC =================

    private fun showGpsProgressDialog() {
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.setPadding(50, 30, 50, 30)
        progressBar.max = 100

        val dialog = AlertDialog.Builder(this)
            .setTitle("Syncing GPS Location")
            .setMessage("Connecting to satellites...")
            .setView(progressBar)
            .setCancelable(false)
            .create()

        dialog.show()

        var progressStatus = 0
        val handler = Handler(Looper.getMainLooper())

        Thread {
            while (progressStatus < 100) {
                progressStatus += 10
                handler.post { progressBar.progress = progressStatus }
                Thread.sleep(200)
            }

            handler.post {
                dialog.dismiss()
                getLastLocation()

                if (currentLocation != null) {
                    val lat = currentLocation!!.latitude
                    val lng = currentLocation!!.longitude

                    showStatusNotification(
                        "Location Synced",
                        "Lat: $lat, Lng: $lng"
                    )
                }

                Toast.makeText(this, "GPS Sync Complete", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    // ================= SCHEDULE =================

    private fun showScheduleDatePicker() {
        val c = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            showScheduleTimePicker(year, month, day)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        datePicker.setTitle("Select Safety Check Date")
        datePicker.show()
    }

    private fun showScheduleTimePicker(year: Int, month: Int, day: Int) {
        val c = Calendar.getInstance()
        val timePicker = TimePickerDialog(this, { _, hour, minute ->
            val scheduledTime = "$day/${month + 1}/$year at $hour:$minute"
            showStatusNotification("Safety Check Scheduled", "Check-in set for $scheduledTime")
            Toast.makeText(this, "Scheduled for: $scheduledTime", Toast.LENGTH_LONG).show()
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)

        timePicker.setTitle("Select Check-in Time")
        timePicker.show()
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
            with(NotificationManagerCompat.from(this)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
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

    // ================= POPUPS & MENUS =================

    private fun showPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.popup_view -> {
                    showStatusNotification("Viewing Contact", "Opening safety profile...")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
