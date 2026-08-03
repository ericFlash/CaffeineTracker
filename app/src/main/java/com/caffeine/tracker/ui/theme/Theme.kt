package com.caffeine.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6F4E37),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDD6C0),
    secondary = Color(0xFFB8860B),
    surface = Color(0xFFFFFBF8),
    background = Color(0xFFFFFBF8),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4A574),
    onPrimary = Color(0xFF3E2A1A),
    primaryContainer = Color(0xFF563F2A),
    secondary = Color(0xFFE8B84B),
    surface = Color(0xFF1C1B1A),
    background = Color(0xFF1C1B1A),
)

@Composable
fun CaffeineTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
