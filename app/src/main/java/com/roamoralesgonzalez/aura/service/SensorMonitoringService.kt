package com.roamoralesgonzalez.aura.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
// Not for now. import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.roamoralesgonzalez.aura.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SensorMonitoringService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var wifiManager: WifiManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var viewModel: MainViewModel
    // Not for now. private val viewModelStore = ViewModelStore()

    // Agregando variables faltantes
    private var lastNotificationTime = 0L
    private val NOTIFICATION_DELAY = 5000L
    private val _magneticFieldStrength = MutableStateFlow(0f)
    private val _isNearDevice = MutableStateFlow(false)

    companion object {
        private const val TAG = "SensorMonitoringService"
        private const val CHANNEL_ID = "AuraSensorService"
        private const val NOTIFICATION_ID = 1
        private var instance: ViewModelStoreOwner? = null

        fun getInstance(): ViewModelStoreOwner {
            return instance ?: object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }.also { instance = it }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeServices()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        viewModel = ViewModelProvider(getInstance())[MainViewModel::class.java]
    }

    private fun initializeServices() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        //fuck you want me to fix this bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (magneticSensor == null) {
            Log.e(TAG, "No magnetic sensor found on this device")
            return
        }

        Log.d(TAG, "Magnetic sensor details:")
        Log.d(TAG, "Name: ${magneticSensor?.name}")
        Log.d(TAG, "Power: ${magneticSensor?.power} mA")
        Log.d(TAG, "Resolution: ${magneticSensor?.resolution} µT")
        Log.d(TAG, "Maximum range: ${magneticSensor?.maximumRange} µT")

        registerSensors()
    }

    private fun registerSensors() {
        magneticSensor?.let {
            val success = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Magnetic sensor registration ${if (success) "successful" else "failed"}")
        }

        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AURA - Monitoreo Activo")
        .setContentText("Monitoreando campos magnéticos")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val strength = calculateMagneticFieldStrength(event.values)
                Log.d(TAG, "Magnetic field strength: $strength µT")
                serviceScope.launch {
                    viewModel.updateMagneticStrength(strength)
                }
                checkMagneticFieldStrength(strength)
            }
            Sensor.TYPE_PROXIMITY -> {
                val isNear = event.values[0] < (proximitySensor?.maximumRange ?: 5f)
                _isNearDevice.value = isNear
                Log.d(TAG, "Proximity changed: ${if (isNear) "near" else "far"}")
                checkProximityWarning()
            }
        }
    }

    private fun calculateMagneticFieldStrength(values: FloatArray): Float {
        val strength = kotlin.math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        return strength
    }

    private fun checkMagneticFieldStrength(strength: Float) {
        // Valores aproximados para marcapasos (en microTesla)
        when {
            strength > 500f -> {
                Log.w(TAG, "Alto nivel de campo magnético detectado: $strength µT")
                showWarning(3)
                disableConnectivity()
            }
            strength > 300f -> {
                Log.w(TAG, "Nivel medio de campo magnético detectado: $strength µT")
                showWarning(2)
            }
            strength > 100f -> {
                Log.w(TAG, "Nivel bajo de campo magnético detectado: $strength µT")
                showWarning(1)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Advertencias AURA"
            val descriptionText = "Canal para advertencias de campos magnéticos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showWarning(level: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < NOTIFICATION_DELAY) {
            return // Evitar spam de notificaciones
        }
        lastNotificationTime = currentTime

        val (title, message) = when (level) {
            1 -> Pair(
                "Precaución - Campo Magnético Detectado",
                "Se detectó un campo magnético débil. Considere alejar el dispositivo."
            )
            2 -> Pair(
                "Advertencia - Campo Magnético Significativo",
                "Campo magnético moderado detectado. Por favor, aleje el dispositivo."
            )
            3 -> Pair(
                "¡ALERTA! - Campo Magnético Peligroso",
                "Campo magnético muy fuerte detectado. Alejando el dispositivo y desactivando conectividad."
            )
            else -> Pair("Alerta de Campo Magnético", "Se detectó actividad magnética anormal.")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + level, notification)
    }

    public fun disableConnectivity() {
        try {
            // Desactivar WiFi
            // desactivamos esto tambien por ahora womp womp wooomp
            // wifiManager.isWifiEnabled = false

            // Desactivar Bluetooth
            // bluetoothAdapter?.disable()
            //VAMOS A DESACTIVAR ESTO POOOOOOOOR AHORA -Camilo

            // Mostrar notificación de confirmación
            showWarning(4) // Nivel especial para confirmación

            Log.i(TAG, "Conectividad desactivada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar la conectividad", e)
        }
    }

    private fun checkProximityWarning() {
        if (_isNearDevice.value) {
            // Si está cerca y hay un campo magnético significativo
            if (_magneticFieldStrength.value > 300f) {
                showWarning(3)
                disableConnectivity()
            } else if (_magneticFieldStrength.value > 100f) {
                showWarning(2)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Accuracy changed for sensor ${sensor?.name}: $accuracy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        sensorManager.unregisterListener(this)
        viewModel.setMonitoring(false)
        instance = null
        Log.d(TAG, "Service destroyed and sensors unregistered")
    }
}
