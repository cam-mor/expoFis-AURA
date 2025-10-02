package com.roamoralesgonzalez.aura.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {
    companion object {
        private const val CHANNEL_ID = "AuraForegroundService"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AURA Monitoreo"
            val descriptionText = "Servicio de monitoreo de campos magnéticos"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AURA está activo")
        .setContentText("Monitoreando campos magnéticos")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    override fun onBind(intent: Intent): IBinder? = null
}
