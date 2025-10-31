package com.example.toda

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TODAApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Enable offline persistence for Firebase Realtime Database
            // Wrap in try-catch to prevent crashes in release builds
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(10 * 1024 * 1024)
                Log.d("TODAApplication", "Firebase persistence enabled successfully")
            } catch (e: Exception) {
                Log.w("TODAApplication", "Firebase persistence setup failed, continuing without persistence", e)
                // Continue without persistence - app will still work
            }

        } catch (e: Exception) {
            Log.e("TODAApplication", "Firebase initialization failed", e)
            // Don't crash the app, let it continue
        }
    }
}
