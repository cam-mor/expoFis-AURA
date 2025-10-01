package com.roamoralesgonzalez.aura

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.roamoralesgonzalez.aura.ui.theme.AURATheme
import androidx.compose.animation.core.*
import com.roamoralesgonzalez.aura.services.FloatingBubbleService

class MainActivity : ComponentActivity() {
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            startFloatingBubble()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkOverlayPermission()
        setContent {
            AURATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
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
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var isMonitoring by remember { mutableStateOf(false) }
    var magneticStrength by remember { mutableStateOf(0f) }
    var warningLevel by remember { mutableStateOf(0) }

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
                onClick = { isMonitoring = !isMonitoring },
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