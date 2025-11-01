package com.example.toda.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.toda.MainActivity
import com.example.toda.R
import com.example.toda.data.Booking

class NotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "booking_notifications"
        private const val ARRIVAL_CHANNEL_ID = "driver_arrival_notifications"
        private const val NOTIFICATION_ID = 1001
        private const val ARRIVAL_NOTIFICATION_ID = 2001
    }

    init {
        createNotificationChannel()
        createArrivalNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Booking Notifications"
            val descriptionText = "Notifications for new booking requests"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createArrivalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Driver Arrival Notifications"
            val descriptionText = "Notifications when your driver arrives at pickup location"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(ARRIVAL_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendBookingNotification(booking: Booking) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Booking Request")
            .setContentText("${booking.customerName} requests a ride from ${booking.pickupLocation}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Customer: ${booking.customerName}\nPhone: ${booking.phoneNumber}\nFrom: ${booking.pickupLocation}\nTo: ${booking.destination}\nFare: ₱${String.format("%.2f", booking.estimatedFare)}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID + booking.id.hashCode(), notification)
        }
    }

    fun sendDriverArrivalNotification(booking: Booking) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("BOOKING_ID", booking.id)
            putExtra("OPEN_ACTIVE_BOOKING", true)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ARRIVAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Driver Has Arrived! 🚗")
            .setContentText("${booking.driverName} (${booking.todaNumber}) is waiting at your pickup location")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your driver ${booking.driverName} (${booking.todaNumber}) has arrived at ${booking.pickupLocation}.\n\nPlease proceed to your pickup location. You have 5 minutes before the booking may be marked as no-show.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(ARRIVAL_NOTIFICATION_ID + booking.id.hashCode(), notification)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun cancelNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun cancelAllNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }
}