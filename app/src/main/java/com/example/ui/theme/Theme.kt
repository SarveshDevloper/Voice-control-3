package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberneticColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepObsidian,
    primaryContainer = ElevatedNavy,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonEmerald,
    onSecondary = DeepObsidian,
    secondaryContainer = Color(0xFF0E2923),
    onSecondaryContainer = NeonEmerald,
    tertiary = WarmAmber,
    onTertiary = DeepObsidian,
    tertiaryContainer = Color(0xFF2D230B),
    onTertiaryContainer = WarmAmber,
    background = DeepObsidian,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedNavy,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = CoralRed,
    onError = DeepObsidian
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberneticColorScheme,
        typography = Typography,
        content = content
    )
}
