package com.testing.adwarecleaner

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RiskHigh = Color(0xFFFF5252)
val RiskMedium = Color(0xFFFFB74D)
val Ok = Color(0xFF3DDC84)
val Accent = Color(0xFFE53935)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1214),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A1214),
    onSecondaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B0B0E),
    onBackground = Color(0xFFECECEE),
    surface = Color(0xFF0B0B0E),
    onSurface = Color(0xFFECECEE),
    surfaceVariant = Color(0xFF24242A),
    onSurfaceVariant = Color(0xFF9D9DA8),
    surfaceContainerLow = Color(0xFF121216),
    surfaceContainer = Color(0xFF16161B),
    surfaceContainerHigh = Color(0xFF1B1B21),
    surfaceContainerHighest = Color(0xFF24242A),
    outline = Color(0xFF3A3A42),
    outlineVariant = Color(0xFF26262D),
    error = RiskHigh,
)

@Composable
fun AdwareTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
