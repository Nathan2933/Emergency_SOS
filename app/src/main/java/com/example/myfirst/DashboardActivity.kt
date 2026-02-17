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
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
