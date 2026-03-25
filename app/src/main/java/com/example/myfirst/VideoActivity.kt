package com.example.myfirst

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Set up MediaController for playback controls
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // Path to the video file (Tutorial MP4)
        // Note: User needs to put tutorial.mp4 in res/raw/
        val videoPath = "android.resource://" + packageName + "/" + R.raw.tutorial
        
        // Alternatively, use a sample URL for testing
        // val videoPath = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
        
        try {
            videoView.setVideoURI(Uri.parse(videoPath))
            videoView.start()
        } catch (e: Exception) {
            // Handle error (e.g., file not found)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
