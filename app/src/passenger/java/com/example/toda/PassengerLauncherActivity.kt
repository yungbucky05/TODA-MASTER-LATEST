package com.example.toda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class PassengerLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("initial_screen", "customer_login")
        }
        startActivity(intent)
        finish()
    }
}

