package com.example.myfirst

import android.app.*
import android.content.Context
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Notification Channel ID is required for Android 8.0+
    private val CHANNEL_ID = "safety_status_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize Notification Channel
        createNotificationChannel()

        // Initialize Buttons from your layout
        val btnSchedule = findViewById<ImageButton>(R.id.imageButton7)
        val btnSync = findViewById<ImageButton>(R.id.imageButton4)
        val btnSOS = findViewById<ImageButton>(R.id.imageButton2)

        // Register buttons for Context Menu (Long Press)
        registerForContextMenu(btnSchedule)
        registerForContextMenu(btnSync)
        registerForContextMenu(btnSOS)

        // 1. ALERT DIALOG: Triggered by SOS Button
        btnSOS.setOnClickListener {
            showSOSConfirmDialog()
        }

        // 2. PROGRESS BAR: Triggered by Sync Button
        btnSync.setOnClickListener {
            showGpsProgressDialog()
        }

        // 3 & 4. DATE & TIME PICKER: Triggered by Schedule Button
        btnSchedule.setOnClickListener {
            showScheduleDatePicker()
        }

        // Existing Long Click Popups
        btnSchedule.setOnLongClickListener { showPopup(it); true }
        btnSync.setOnLongClickListener { showPopup(it); true }
        btnSOS.setOnLongClickListener { showPopup(it); true }
    }

    // --- 1. ALERT DIALOG (Emergency Confirmation) ---
    private fun showSOSConfirmDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm SOS Alert")
        builder.setMessage("Are you sure you want to notify all emergency contacts of your current location?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("SEND SOS") { _, _ ->
            // 5. STATUS NOTIFICATION: Triggered after confirmation
            showStatusNotification("SOS DISPATCHED", "Your emergency contacts have been alerted.")
            Toast.makeText(this, "SOS Sent!", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // --- 2. PROGRESS BAR (Simulating GPS Location Sync) ---
    private fun showGpsProgressDialog() {
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.setPadding(50, 30, 50, 30)
        progressBar.isIndeterminate = false
        progressBar.max = 100

        val dialog = AlertDialog.Builder(this)
            .setTitle("Syncing GPS Location")
            .setMessage("Connecting to emergency satellites...")
            .setView(progressBar)
            .setCancelable(false) // User must wait for sync
            .create()

        dialog.show()

        // Simulate progress on a background thread
        var progressStatus = 0
        val handler = Handler(Looper.getMainLooper())
        Thread {
            while (progressStatus < 100) {
                progressStatus += 10
                handler.post { progressBar.progress = progressStatus }
                try { Thread.sleep(250) } catch (e: Exception) { e.printStackTrace() }
            }
            handler.post {
                dialog.dismiss()
                showStatusNotification("Location Synced", "Your GPS coordinates are now live.")
                Toast.makeText(this, "GPS Sync Complete", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    // --- 3. DATE PICKER (Scheduling a Safety Check) ---
    private fun showScheduleDatePicker() {
        val c = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            // Once date is picked, immediately show Time Picker
            showScheduleTimePicker(year, month, day)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        datePicker.setTitle("Select Safety Check Date")
        datePicker.show()
    }

    // --- 4. TIME PICKER (Part of Scheduling) ---
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

    // --- 5. STATUS NOTIFICATION (System Tray) ---
    private fun showStatusNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Safety Icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(this)) {
                // Use unique ID (current time) to allow multiple notifications
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Create the Notification Channel (Required for API 26+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Safety Alerts"
            val descriptionText = "Notifications for SOS and GPS status"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- MENU LOGIC ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                showSOSConfirmDialog() // Reuse Alert Dialog for "About/SOS"
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete -> {
                Toast.makeText(this, "Contact removed from safety list", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
}