package com.roamoralesgonzalez.aura.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.roamoralesgonzalez.aura.MainActivity

class ConnectivityManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas AURA"
            val descriptionText = "Canal para alertas de proximidad a marcapasos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // La vibración se maneja a través del NotificationCompat.Builder
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 250, 500), -1)
        }
    }

    fun showWarning(message: String, isUrgent: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

        notificationManager.notify(notificationId++, builder.build())
    }

    fun disableConnectivity() {
        try {
            // Desactivar WiFi
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = false

            // Verificar permisos de Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothAdapter?.disable()
                }
            } else {
                bluetoothAdapter?.disable()
            }

        } catch (e: SecurityException) {
            showWarning("No se pueden desactivar las conexiones. Por favor, revise los permisos.", true)
        }
    }

    fun enableConnectivity() {
        try {
            // Reactivar WiFi
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = true

            // Verificar permisos de Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothAdapter?.enable()
                }
            } else {
                bluetoothAdapter?.enable()
            }

        } catch (e: SecurityException) {
            showWarning("No se pueden reactivar las conexiones. Por favor, revise los permisos.", false)
        }
    }
}
