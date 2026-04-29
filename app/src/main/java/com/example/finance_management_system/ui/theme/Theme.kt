package com.example.finance_management_system.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepOcean,
    onPrimary = Snow,
    primaryContainer = SoftSky,
    onPrimaryContainer = MidnightNavy,
    secondary = AuroraMint,
    onSecondary = MidnightNavy,
    tertiary = CoralRed,
    onTertiary = Snow,
    background = Mist,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = Cloud,
    onSurfaceVariant = Steel,
    outline = Color(0xFFD4DDEB),
    outlineVariant = Color(0xFFE8EEF6),
    error = CoralRed,
    onError = Snow,
)

private val DarkColors = darkColorScheme(
    primary = AuroraMint,
    onPrimary = MidnightNavy,
    primaryContainer = SlateBlue,
    onPrimaryContainer = NightText,
    secondary = SoftSky,
    onSecondary = MidnightNavy,
    tertiary = CoralRed,
    onTertiary = NightText,
    background = MidnightNavy,
    onBackground = NightText,
    surface = DeepOcean,
    onSurface = NightText,
    surfaceVariant = SlateBlue,
    onSurfaceVariant = NightMuted,
    outline = Color(0xFF33415C),
    outlineVariant = Color(0xFF27324A),
    error = CoralRed,
    onError = NightText,
)

@Composable
fun MyFinancialTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
