import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.layout.layout
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private val systemReceiver = SystemReceiver()
    private val SMS_PERMISSION_CODE = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Existing Buttons
        val btnSms = findViewById<androidx.compose.material3.Button>(R.id.btnSendSMS)
        val btnWhatsapp = findViewById<androidx.compose.material3.Button>(R.id.btnSendWhatsapp)
        val btnTeam = findViewById<androidx.compose.material3.Button>(R.id.btnTeamPage)

        btnSms.setOnClickListener { sendSMSAlert() }
        btnWhatsapp.setOnClickListener { sendWhatsAppAlert() }
        btnTeam.setOnClickListener {
            startActivity(Intent(this, TeamActivity::class.java))
        }
    }

    // ================= SMS LOGIC =================
    private fun sendSMSAlert() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(
                    "1234567890",
                    null,
                    "Emergency! I need help immediately.",
                    null,
                    null
                )
                android.widget.Toast.makeText(this, "Alert SMS Sent!", Toast.LENGTH_SHORT).show()
            } catch (e: java.lang.Exception) {
                android.widget.Toast.makeText(this, "SMS Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    // ================= WHATSAPP LOGIC =================
    private fun sendWhatsAppAlert() {
        val message = "Emergency Alert! Please check my location."
        val url = "https://api.whatsapp.com/send?phone=1234567890&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
        startActivity(intent)
    }

    // ================= BROADCAST RECEIVER LIFECYCLE =================
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        registerReceiver(systemReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(systemReceiver)
    }
}