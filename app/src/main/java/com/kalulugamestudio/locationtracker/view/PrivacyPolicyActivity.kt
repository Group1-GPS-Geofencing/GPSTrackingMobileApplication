package com.kalulugamestudio.locationtracker.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kalulugamestudio.locationtracker.R

class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        val acceptButton: Button = findViewById(R.id.acceptButton)
        val rejectButton: Button = findViewById(R.id.rejectButton)

        acceptButton.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            startActivity(intent)
            finish()
        }

        rejectButton.setOnClickListener {
            finish()
        }
    }
}