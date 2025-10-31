package com.example.toda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class AdminLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("initial_screen", "admin_login")
        }
        startActivity(intent)
        finish()
    }
}

