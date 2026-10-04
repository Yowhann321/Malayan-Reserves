package com.example.malayanreserves.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueSoft,
    onPrimaryContainer = Navy,
    secondary = Yellow,
    onSecondary = Navy,
    secondaryContainer = YellowSoft,
    onSecondaryContainer = Navy,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Muted,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    outline = Line,
    outlineVariant = Line,
    error = Red,
    errorContainer = RedSoft
)

@Composable
fun MalayanReservesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Typography, content = content)
}
