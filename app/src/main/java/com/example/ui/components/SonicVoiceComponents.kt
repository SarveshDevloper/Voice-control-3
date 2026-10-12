package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SonicVoiceVisualizerOrb(
    isListening: Boolean,
    continuousRadarMode: Boolean,
    rmsLevel: Float,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonic_orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 650 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val activeColor = when {
        isListening -> NeonEmerald
        continuousRadarMode -> ElectricCyan
        else -> ElectricCyan
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(188.dp)
                .clip(CircleShape)
                .clickable(onClick = onMicClick)
                .testTag("sonic_mic_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.28f
                val dynamicBoost = if (isListening) (rmsLevel * 24f) else 4f

                // Outer glowing acoustic rings
                for (ring in 3 downTo 1) {
                    val ringRadius = baseRadius * (1f + ring * 0.24f * pulseScale) + dynamicBoost
                    drawCircle(
                        color = activeColor.copy(alpha = (0.24f / ring)),
                        radius = ringRadius,
                        center = center
                    )
                    drawCircle(
                        color = activeColor.copy(alpha = (0.45f / ring)),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 24 radial equalizer bars around the orb
                val barCount = 24
                for (i in 0 until barCount) {
                    val angle = (i.toFloat() / barCount) * (2f * Math.PI.toFloat()) + wavePhase * 0.35f
                    val waveFactor = if (isListening) {
                        0.45f + 0.55f * kotlin.math.abs(sin(wavePhase * 2f + i * 0.6f)) * (0.5f + rmsLevel)
                    } else {
                        0.22f + 0.12f * kotlin.math.abs(sin(wavePhase + i * 0.5f))
                    }
                    val innerR = baseRadius * 1.15f
                    val outerR = innerR + (size.minDimension * 0.14f * waveFactor)
                    val startOffset = Offset(
                        x = center.x + cos(angle) * innerR,
                        y = center.y + sin(angle) * innerR
                    )
                    val endOffset = Offset(
                        x = center.x + cos(angle) * outerR,
                        y = center.y + sin(angle) * outerR
                    )
                    drawLine(
                        color = if (i % 2 == 0) ElectricCyan else NeonEmerald,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Core Orb Circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.95f),
                            activeColor.copy(alpha = 0.45f),
                            DeepObsidian
                        ),
                        center = center,
                        radius = baseRadius * 1.15f
                    ),
                    radius = baseRadius,
                    center = center
                )
            }

            Icon(
                imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.MicNone,
                contentDescription = "Tap to speak voice command",
                tint = DeepObsidian,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Animated Horizontal Equalizer Strip
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(24.dp)
        ) {
            val bars = 28
            val spacing = 4.dp.toPx()
            val totalSpacing = spacing * (bars - 1)
            val barWidth = ((size.width - totalSpacing) / bars).coerceAtLeast(3f)
            for (i in 0 until bars) {
                val wave = kotlin.math.abs(sin(wavePhase * 1.8f + i * 0.45f))
                val factor = if (isListening) (0.25f + 0.75f * wave) else (0.15f + 0.2f * wave)
                val barHeight = (size.height * factor).coerceAtLeast(4.dp.toPx())
                val x = i * (barWidth + spacing)
                val y = (size.height - barHeight) / 2f
                drawRoundRect(
                    color = if (isListening) NeonEmerald else ElectricCyan.copy(alpha = 0.65f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
fun LiveSystemTelemetryRibbon(
    isAccessibilityConnected: Boolean,
    batteryPercent: Int,
    isCharging: Boolean,
    volumePercent: Int,
    isTorchOn: Boolean,
    continuousRadarMode: Boolean,
    onAccessibilityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Accessibility Status Chip
        TelemetryChip(
            icon = Icons.Filled.VerifiedUser,
            label = if (isAccessibilityConnected) "System Ready" else "Enable Access",
            accentColor = if (isAccessibilityConnected) NeonEmerald else WarmAmber,
            onClick = onAccessibilityClick,
            modifier = Modifier
                .weight(1.25f)
                .testTag("telemetry_accessibility_chip")
        )

        // Battery Status Chip
        TelemetryChip(
            icon = if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
            label = "$batteryPercent%",
            accentColor = ElectricCyan,
            onClick = null,
            modifier = Modifier.weight(0.85f)
        )

        // Volume Status Chip
        TelemetryChip(
            icon = Icons.Filled.VolumeUp,
            label = "$volumePercent%",
            accentColor = ElectricCyan,
            onClick = null,
            modifier = Modifier.weight(0.85f)
        )

        // Torch / Radar Indicator Chip
        TelemetryChip(
            icon = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Filled.Radar,
            label = when {
                isTorchOn -> "Torch ON"
                continuousRadarMode -> "Radar ON"
                else -> "Standby"
            },
            accentColor = if (isTorchOn || continuousRadarMode) NeonEmerald else TextSecondary,
            onClick = null,
            modifier = Modifier.weight(1.05f)
        )
    }
}

@Composable
private fun TelemetryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val chipModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.clip(RoundedCornerShape(14.dp))
    }

    Surface(
        modifier = chipModifier.border(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.35f),
            shape = RoundedCornerShape(14.dp)
        ),
        color = CardSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
