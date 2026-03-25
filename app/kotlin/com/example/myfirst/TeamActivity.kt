import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.layout.layout

class TeamActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team)

        val imgMember1 = findViewById<ImageView>(R.id.member1)
        val imgMember2 = findViewById<ImageView>(R.id.member2)
        val btnPlay = findViewById<Button>(R.id.btnPlayMusic)

        // 1. Rotation Animation
        imgMember1.setOnClickListener {
            ObjectAnimator.ofFloat(imgMember1, "rotation", 0f, 360f).setDuration(1000).start()
        }

        // 2. Scale Animation
        imgMember2.setOnClickListener {
            ObjectAnimator.ofFloat(imgMember2, "scaleX", 1f, 1.5f, 1f).setDuration(500).start()
            ObjectAnimator.ofFloat(imgMember2, "scaleY", 1f, 1.5f, 1f).setDuration(500).start()
        }

        // 3. Playlist Logic (Using System Ringtones)
        btnPlay.setOnClickListener {
            mediaPlayer?.stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}