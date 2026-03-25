
package com.example.myfirst

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.*
import android.view.animation.AnimationUtils
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

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "safety_status_channel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val LOCATION_PERMISSION_CODE = 101
    private val NOTIFICATION_PERMISSION_CODE = 102
    
    private var mediaPlayer: MediaPlayer? = null
    private var isSirenPlaying = false

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        createNotificationChannel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()
        requestNotificationPermission()

        val rowCall = findViewById<LinearLayout>(R.id.rowCall)
        val rowSOS = findViewById<LinearLayout>(R.id.rowSOS)
        
        // Start Pulse Animation on SOS Row
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        rowSOS.startAnimation(pulse)
        val rowLocation = findViewById<LinearLayout>(R.id.rowLocation)
        val rowSMS = findViewById<LinearLayout>(R.id.rowSMS)
        val rowWhatsApp = findViewById<LinearLayout>(R.id.rowWhatsApp)
        val rowSetContact = findViewById<LinearLayout>(R.id.rowSetContact)
        val rowVideo = findViewById<LinearLayout>(R.id.rowVideo)

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

        rowSMS.setOnClickListener {
            val sharedPrefs = getSharedPreferences("EmergencySOS", MODE_PRIVATE)
            val savedContact = sharedPrefs.getString("emergency_contact", "")
            if (!savedContact.isNullOrEmpty()) {
                val message = "EMERGENCY! I need help. My location: ${currentLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "Not available"}"
                sendSMS(savedContact, message)
            } else {
                promptForContactAndSend("SMS")
            }
        }

        rowWhatsApp.setOnClickListener {
            val sharedPrefs = getSharedPreferences("EmergencySOS", MODE_PRIVATE)
            val savedContact = sharedPrefs.getString("emergency_contact", "")
            if (!savedContact.isNullOrEmpty()) {
                val message = "EMERGENCY! I need help. My location: ${currentLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "Not available"}"
                sendWhatsAppMessage(savedContact, message)
            } else {
                promptForContactAndSend("WhatsApp")
            }
        }

        rowSetContact.setOnClickListener {
            showSetContactDialog()
        }

        rowVideo.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java))
        }
    }

    private fun showSetContactDialog() {
        val input = EditText(this)
        val sharedPrefs = getSharedPreferences("EmergencySOS", MODE_PRIVATE)
        input.setText(sharedPrefs.getString("emergency_contact", ""))
        input.hint = "Enter Phone Number"

        AlertDialog.Builder(this)
            .setTitle("Set Emergency Contact")
            .setMessage("This number will be used for automatic alerts.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val phoneNumber = input.text.toString()
                sharedPrefs.edit().putString("emergency_contact", phoneNumber).apply()
                Toast.makeText(this, "Emergency contact saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptForContactAndSend(type: String) {
        val input = EditText(this)
        input.hint = "Enter Phone Number"
        
        AlertDialog.Builder(this)
            .setTitle("Send $type Alert")
            .setMessage("Enter the phone number of your friend:")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val phoneNumber = input.text.toString()
                if (phoneNumber.isNotEmpty()) {
                    val message = "EMERGENCY! I need help. My location: ${currentLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "Not available"}"
                    if (type == "SMS") {
                        sendSMS(phoneNumber, message)
                    } else {
                        sendWhatsAppMessage(phoneNumber, message)
                    }
                } else {
                    Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.getSystemService(SmsManager::class.java)
                } else {
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "SMS Sent to $phoneNumber", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 103)
        }
    }

    private fun sendWhatsAppMessage(phoneNumber: String, message: String) {
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse(url)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
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
                    toggleSiren()
                    Toast.makeText(this, "SOS Sent & Siren Triggered!", Toast.LENGTH_LONG).show()
                } ?: Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleSiren() {
        if (isSirenPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isSirenPlaying = false
            Toast.makeText(this, "Siren Stopped", Toast.LENGTH_SHORT).show()
        } else {
            try {
                // Using a system sound as a fallback siren or tell user to add R.raw.siren
                mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                isSirenPlaying = true
            } catch (e: Exception) {
                Toast.makeText(this, "Error playing siren", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
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
}