package com.roamoralesgonzalez.aura.service

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorMonitoringService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    private val _magneticFieldStrength = MutableStateFlow(0f)
    private val _isNearDevice = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Registrar los sensores
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val strength = calculateMagneticFieldStrength(event.values)
                _magneticFieldStrength.value = strength
                checkMagneticFieldStrength(strength)
            }
            Sensor.TYPE_PROXIMITY -> {
                _isNearDevice.value = (event.values[0] < (proximitySensor?.maximumRange ?: 5f))
                checkProximityWarning()
            }
        }
    }

    private fun calculateMagneticFieldStrength(values: FloatArray): Float {
        return kotlin.math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    private fun checkMagneticFieldStrength(strength: Float) {
        // Valores aproximados para marcapasos (en microTesla)
        when {
            strength > 500f -> {
                // Nivel de advertencia alto - Campo magnético muy fuerte
                showWarning(3)
                disableConnectivity()
            }
            strength > 300f -> {
                // Nivel de advertencia medio
                showWarning(2)
            }
            strength > 100f -> {
                // Nivel de advertencia bajo
                showWarning(1)
            }
        }
    }

    private fun showWarning(level: Int) {
        // TODO: Implementar sistema de notificaciones
    }

    private fun disableConnectivity() {
        // TODO: Implementar desactivación de conectividad
    }

    private fun checkProximityWarning() {
        // Lógica para verificar la proximidad y mostrar advertencias si es necesario
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
