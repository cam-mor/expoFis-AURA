package com.roamoralesgonzalez.aura

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.roamoralesgonzalez.aura.service.SensorMonitoringService
import com.roamoralesgonzalez.aura.services.FloatingBubbleService
import com.roamoralesgonzalez.aura.ui.theme.AURATheme
import com.roamoralesgonzalez.aura.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    private val requiredPermissions = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private lateinit var sensorIntent: Intent
    private var isServiceRunning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkOverlayPermission()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Settings.canDrawOverlays(this)) {
            startFloatingBubble()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        sensorIntent = Intent(this, SensorMonitoringService::class.java)
        requestPermissions()
        setContent {
            AURATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val magneticStrength by viewModel.magneticStrength.collectAsState()
                    val warningLevel by viewModel.warningLevel.collectAsState()
                    val isMonitoring by viewModel.isMonitoring.collectAsState()

                    MainScreen(
                        magneticStrength = magneticStrength,
                        warningLevel = warningLevel,
                        isMonitoring = isMonitoring,
                        onMonitoringChanged = { monitoring ->
                            viewModel.setMonitoring(monitoring)
                            if (monitoring) startSensorService() else stopSensorService()
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startFloatingBubble()
        }
    }

    private fun startFloatingBubble() {
        val serviceIntent = Intent(this, FloatingBubbleService::class.java)
        startService(serviceIntent)
    }

    private fun startSensorService() {
        if (!isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(sensorIntent)
            } else {
                startService(sensorIntent)
            }
            isServiceRunning = true
        }
    }

    private fun stopSensorService() {
        if (isServiceRunning) {
            stopService(sensorIntent)
            isServiceRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorService()
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    magneticStrength: Float = 0f,
    warningLevel: Int = 0,
    isMonitoring: Boolean = false,
    onMonitoringChanged: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Título
        Text(
            text = "AURA",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Indicador visual
        MagneticFieldIndicator(
            strength = magneticStrength,
            warningLevel = warningLevel,
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        )

        // Estado actual
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Estado del sensor:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isMonitoring) "Monitorizando" else "Detenido",
                    color = if (isMonitoring) Color.Green else Color.Red
                )
                Text(
                    text = "Intensidad: %.2f µT".format(magneticStrength),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Nivel de advertencia: $warningLevel",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Controles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onMonitoringChanged(!isMonitoring) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) Color.Red else Color.Green
                )
            ) {
                Text(if (isMonitoring) "Detener" else "Iniciar")
            }

            Button(
                onClick = { /* Implementar configuración */ }
            ) {
                Text("Configuración")
            }
        }
    }
}

@Composable
fun MagneticFieldIndicator(
    strength: Float,
    warningLevel: Int,
    modifier: Modifier = Modifier
) {
    val animatedStrength by animateFloatAsState(
        targetValue = strength,
        animationSpec = tween(durationMillis = 500)
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2

        // Dibujar círculo base
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.3f),
            radius = radius,
            center = Offset(canvasWidth / 2, canvasHeight / 2)
        )

        // Dibujar indicador de intensidad
        val sweepAngle = (animatedStrength / 100f) * 360f
        drawArc(
            color = when (warningLevel) {
                0 -> Color.Green
                1 -> Color.Yellow
                2 -> Color.Red
                else -> Color.Red
            },
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = true,
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(
                (canvasWidth - radius * 2) / 2,
                (canvasHeight - radius * 2) / 2
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AURATheme {
        MainScreen()
    }
}