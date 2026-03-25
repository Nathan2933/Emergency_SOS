package com.example.myfirst

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast

class EmergencyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("EmergencySOS", Context.MODE_PRIVATE)
        val emergencyContact = sharedPrefs.getString("emergency_contact", "")

        when (intent.action) {
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isAirplaneModeOn = intent.getBooleanExtra("state", false)
                val status = if (isAirplaneModeOn) "ON" else "OFF"
                val message = "ALERT: Airplane Mode is $status"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                
                if (!emergencyContact.isNullOrEmpty()) {
                    sendAutomaticSMS(context, emergencyContact, message)
                }
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = level * 100 / scale.toFloat()
                
                if (batteryPct <= 15) {
                    val message = "Warning: Low Battery ($batteryPct%)!"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    
                    if (!emergencyContact.isNullOrEmpty()) {
                        sendAutomaticSMS(context, emergencyContact, message)
                    }
                }
            }
        }
    }

    private fun sendAutomaticSMS(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            // No toast for sending to avoid cluttering in background, maybe just a log or small toast
            // Toast.makeText(context, "Automatic Alert sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Silently fail or log
        }
    }
}
