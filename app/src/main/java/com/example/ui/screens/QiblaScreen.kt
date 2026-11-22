package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.sensors.CompassAccuracy
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.SoftGoldAccent
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.PriyoAzanUiState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    state: PriyoAzanUiState,
    onStartCompass: () -> Unit,
    onStopCompass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current

    DisposableEffect(Unit) {
        onStartCompass()
        onDispose { onStopCompass() }
    }

    val qiblaBearing = state.qiblaInfo.bearingDegrees.toFloat()
    val deviceHeading = state.compassReading.azimuthDegrees
    val dialRotation by animateFloatAsState(
        targetValue = if (deviceHeading != null) -deviceHeading else 0f,
        animationSpec = spring(stiffness = 220f),
        label = "compassDialRotation"
    )

    val diffDeg = if (deviceHeading != null) {
        val raw = ((qiblaBearing - deviceHeading + 540f) % 360f) - 180f
        abs(raw)
    } else {
        180f
    }
    val isAligned = deviceHeading != null && diffDeg <= 4.0f

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.qiblaTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isAligned) StatusSuccess.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = when {
                        !state.compassReading.sensorAvailable -> strings.sensorUnavailable
                        isAligned -> strings.alignedWithQibla
                        else -> strings.rotateDeviceToAlign
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isAligned) StatusSuccess else MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // Geometric Compass Dial
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 320.dp)
                    .aspectRatio(1f)
                    .testTag("qibla_compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.minDimension * 0.45f

                    // Outer bezel
                    drawCircle(
                        color = surfaceColor,
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = if (isAligned) StatusSuccess else primaryColor,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 10f)
                    )

                    // Rotating rose + Qibla needle
                    rotate(degrees = dialRotation, pivot = Offset(cx, cy)) {
                        for (deg in 0 until 360 step 15) {
                            val rad = (deg - 90) * PI / 180.0
                            val isCardinal = deg % 90 == 0
                            val inner = radius * if (isCardinal) 0.78f else 0.87f
                            val outer = radius * 0.94f
                            drawLine(
                                color = if (deg == 0) Color(0xFFD64545) else outlineColor,
                                start = Offset(
                                    cx + (inner * cos(rad)).toFloat(),
                                    cy + (inner * sin(rad)).toFloat()
                                ),
                                end = Offset(
                                    cx + (outer * cos(rad)).toFloat(),
                                    cy + (outer * sin(rad)).toFloat()
                                ),
                                strokeWidth = if (isCardinal) 5f else 2.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Qibla Kaaba pointer at bearing angle
                        rotate(degrees = qiblaBearing, pivot = Offset(cx, cy)) {
                            val pointerPath = Path().apply {
                                moveTo(cx, cy - radius * 0.82f)
                                lineTo(cx - radius * 0.09f, cy)
                                lineTo(cx + radius * 0.09f, cy)
                                close()
                            }
                            drawPath(
                                path = pointerPath,
                                color = if (isAligned) StatusSuccess else SoftGoldAccent
                            )
                            // Kaaba square marker near tip
                            val kSize = radius * 0.16f
                            drawRect(
                                color = Color(0xFF1A1A1A),
                                topLeft = Offset(cx - kSize / 2f, cy - radius * 0.76f),
                                size = androidx.compose.ui.geometry.Size(kSize, kSize)
                            )
                            drawLine(
                                color = SoftGoldAccent,
                                start = Offset(cx - kSize / 2f, cy - radius * 0.76f + kSize * 0.35f),
                                end = Offset(cx + kSize / 2f, cy - radius * 0.76f + kSize * 0.35f),
                                strokeWidth = 4f
                            )
                        }
                    }

                    // Center hub
                    drawCircle(
                        color = primaryColor,
                        radius = radius * 0.08f,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Metrics cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.qiblaBearing,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = AppStrings.localizeDigits("${qiblaBearing.roundToInt()}°", lang),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.distanceToKaaba,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = AppStrings.localizeDigits("${state.qiblaInfo.distanceKm.roundToInt()} km", lang),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Sensor accuracy & calibration guidance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val accText = when (state.compassReading.accuracy) {
                        CompassAccuracy.HIGH -> strings.sensorAccuracyHigh
                        CompassAccuracy.MEDIUM -> strings.sensorAccuracyMedium
                        CompassAccuracy.LOW -> strings.sensorAccuracyLow
                        CompassAccuracy.UNAVAILABLE -> strings.sensorUnavailable
                    }
                    Text(
                        text = accText,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.compassReading.accuracy == CompassAccuracy.LOW) StatusWarning
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.calibrationTip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.location?.let { loc ->
                        Text(
                            text = "${loc.name} (${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)})",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
