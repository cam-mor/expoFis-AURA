package com.roamoralesgonzalez.aura.service

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.content.Context
import com.roamoralesgonzalez.aura.util.ConnectivityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorMonitoringService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var connectivityManager: ConnectivityManager
    private var magnetometer: Sensor? = null
    private var proximitySensor: Sensor? = null

    private val _magneticFieldStrength = MutableStateFlow(0f)
    val magneticFieldStrength: StateFlow<Float> = _magneticFieldStrength

    private val _isNearDevice = MutableStateFlow(false)
    val isNearDevice: StateFlow<Boolean> = _isNearDevice

    private var warningLevel = 0
    private val MAGNETIC_THRESHOLD = 50f // Umbral en microTesla (ajustable)
    private val WARNING_INTERVAL = 10000L // 5 segundos entre advertencias

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        connectivityManager = ConnectivityManager(this)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        registerSensors()
    }

    private fun registerSensors() {
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val magnitude = calculateMagnitude(event.values)
                _magneticFieldStrength.value = magnitude
                checkMagneticThreshold(magnitude)
            }
            Sensor.TYPE_PROXIMITY -> {
                _isNearDevice.value = event.values[0] < proximitySensor?.maximumRange ?: 5f
            }
        }
    }

    private fun calculateMagnitude(values: FloatArray): Float {
        return kotlin.math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    private fun checkMagneticThreshold(magnitude: Float) {
        if (magnitude > MAGNETIC_THRESHOLD && _isNearDevice.value) {
            warningLevel++
            when (warningLevel) {
                1 -> {
                    connectivityManager.showWarning(
                        "¡Precaución! Detectado posible marcapasos cerca. Por favor, aleje el dispositivo.",
                        false
                    )
                }
                2 -> {
                    connectivityManager.showWarning(
                        "¡Segunda advertencia! Campo magnético elevado detectado. Aleje el dispositivo inmediatamente.",
                        true
                    )
                }
                3 -> {
                    connectivityManager.showWarning(
                        "¡Advertencia final! Desactivando conectividad por seguridad.",
                        true
                    )
                    connectivityManager.disableConnectivity()
                }
            }
        } else if (magnitude < MAGNETIC_THRESHOLD) {
            if (warningLevel >= 3) {
                connectivityManager.enableConnectivity()
            }
            warningLevel = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
