package com.roamoralesgonzalez.aura.util

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.roamoralesgonzalez.aura.MainActivity
import android.app.PendingIntent

@Suppress("DEPRECATION")
class ConnectivityManager(private val context: Context) {
    private val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val CHANNEL_ID = "AURA_WARNINGS"
    private var notificationId = 0

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Alertas AURA"
        val descriptionText = "Canal para alertas de proximidad a marcapasos"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 500, 250, 500)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun showWarning(message: String, isUrgent: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Alerta AURA")
            .setContentText(message)
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isUrgent) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
            vibrate()
        }

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .notify(notificationId++, builder.build())
    }

    fun disableConnectivity() {
        disableWifi()
        disableBluetooth()
        vibrate()
    }

    fun enableConnectivity() {
        enableWifi()
        enableBluetooth()
    }

    private fun disableWifi() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+)
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(panelIntent)
            } else {
                // Para versiones anteriores
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            showWarning("Por favor, desactive el WiFi manualmente")
        } catch (_: Exception) {
            showWarning("No se pudo acceder a la configuración de WiFi")
        }
    }

    private fun enableWifi() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+)
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(panelIntent)
            } else {
                // Para versiones anteriores
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            showWarning("Por favor, active el WiFi manualmente")
        } catch (_: Exception) {
            showWarning("No se pudo acceder a la configuración de WiFi")
        }
    }

    private fun disableBluetooth() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showWarning("Por favor, desactive el Bluetooth manualmente")
        } catch (_: Exception) {
            showWarning("No se pudo acceder a la configuración de Bluetooth")
        }
    }

    private fun enableBluetooth() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showWarning("Por favor, active el Bluetooth manualmente")
        } catch (_: Exception) {
            showWarning("No se pudo acceder a la configuración de Bluetooth")
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no se necesita este permiso específico
        }
    }
}
