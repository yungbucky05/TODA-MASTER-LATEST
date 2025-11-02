package com.example.toda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Launcher activity specifically for the Barker build variant.
 * This bypasses the user type selection and goes directly to barker login.
 */
class BarkerLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch MainActivity with barker_login as initial screen
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("initial_screen", "barker_login")
        startActivity(intent)
        finish()
    }
}

