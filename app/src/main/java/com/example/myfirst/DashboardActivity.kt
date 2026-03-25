package com.example.myfirst

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.telephony.SmsManager
import android.net.Uri
class DashboardActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize SharedPreferences & FirebaseAuth
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()

        sharedPreferences.edit().putString("userEmail", FirebaseAuth.getInstance().currentUser?.email).apply()
        // UI References
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        val btnProfile = findViewById<MaterialButton>(R.id.btnProfile)
        val btnSettings = findViewById<MaterialButton>(R.id.btnSettings)

        // Retrieve and Display User Email
        val userEmail = sharedPreferences.getString("userEmail", null)
        tvWelcome.text = "Welcome, ${userEmail ?: "User"}"

        // Logout User
        btnLogout.setOnClickListener { logoutUser() }

        // Placeholder Click Listeners
        btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile Clicked!", Toast.LENGTH_SHORT).show()
            // Navigate to Profile Activity (if exists)
            // startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings Clicked!", Toast.LENGTH_SHORT).show()
            // Navigate to Settings Activity (if exists)
            // startActivity(Intent(this, SettingsActivity::class.java))
        }

        val btnSms = findViewById<MaterialButton>(R.id.btnSms)
        val btnWhatsapp = findViewById<MaterialButton>(R.id.btnWhatsapp)
        val btnTeam = findViewById<MaterialButton>(R.id.btnTeam)

        btnSms.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 101)
            } else {
                sendSmsAlert()
            }
        }

        btnWhatsapp.setOnClickListener {
            sendWhatsAppMessage()
        }

        btnTeam.setOnClickListener {
            startActivity(Intent(this, TeamActivity::class.java))
        }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun sendSmsAlert() {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage("1234567890", null, "Alert! I need help.", null, null)
            Toast.makeText(this, "SMS Sent Successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun sendWhatsAppMessage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("http://api.whatsapp.com/send?phone=1234567890&text=Alert! I need help.")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendSmsAlert()
        } else {
            Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
}
