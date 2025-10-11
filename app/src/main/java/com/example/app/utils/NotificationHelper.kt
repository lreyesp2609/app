package com.example.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log

object NotificationHelper {
    const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Recordatorios"
    private const val CHANNEL_DESCRIPTION = "Notificaciones de recordatorios"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                lightColor = Color.BLUE
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            Log.d("NotificationHelper", "✅ Canal de notificaciones creado")
        }
    }
}