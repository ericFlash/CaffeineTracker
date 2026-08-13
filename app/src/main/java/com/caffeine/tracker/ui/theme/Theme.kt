package com.caffeine.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 浅咖啡色系 — 暖色调咖啡主题
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6F4E37),          // 咖啡棕色
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDD6C0),  // 浅奶咖
    onPrimaryContainer = Color(0xFF3E2A1A),
    secondary = Color(0xFFA67B5B),         // 焦糖色
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0DBCC), // 浅焦糖
    onSecondaryContainer = Color(0xFF3E2A1A),
    tertiary = Color(0xFF8B6F5E),          // 可可棕
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E6DB),
    onTertiaryContainer = Color(0xFF3A2A1D),
    background = Color(0xFFFDF6F0),        // 奶油白底
    onBackground = Color(0xFF2C1F16),
    surface = Color(0xFFFDF6F0),           // 奶油白表面
    onSurface = Color(0xFF2C1F16),
    surfaceVariant = Color(0xFFF0E5DC),
    onSurfaceVariant = Color(0xFF5D4A3C),
    surfaceDim = Color(0xFFE9DDD2),
    surfaceBright = Color(0xFFFFFBF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F0E8),
    surfaceContainer = Color(0xFFF3E9DF),
    surfaceContainerHigh = Color(0xFFEDE3D8),
    surfaceContainerHighest = Color(0xFFE7DCD1),
    outline = Color(0xFFB8A99C),
    outlineVariant = Color(0xFFE0D5CC),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF4A1D1D),
)

// 深色咖啡色系
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4A574),           // 浅咖啡
    onPrimary = Color(0xFF3E2A1A),
    primaryContainer = Color(0xFF563F2A),   // 深咖啡
    onPrimaryContainer = Color(0xFFEDD6C0),
    secondary = Color(0xFFE8B84B),         // 金色
    onSecondary = Color(0xFF3E2A1A),
    secondaryContainer = Color(0xFF6B4F35),
    onSecondaryContainer = Color(0xFFF8E3CD),
    tertiary = Color(0xFFC4A68D),
    onTertiary = Color(0xFF3A2A1D),
    tertiaryContainer = Color(0xFF574235),
    onTertiaryContainer = Color(0xFFEFDCCB),
    background = Color(0xFF1C1815),        // 深褐色底
    onBackground = Color(0xFFE8E0D8),
    surface = Color(0xFF1C1815),
    onSurface = Color(0xFFE8E0D8),
    surfaceVariant = Color(0xFF2C241F),
    onSurfaceVariant = Color(0xFFC4B8AC),
    surfaceDim = Color(0xFF1C1815),
    surfaceBright = Color(0xFF3A332C),
    surfaceContainerLowest = Color(0xFF171310),
    surfaceContainerLow = Color(0xFF25201B),
    surfaceContainer = Color(0xFF29241F),
    surfaceContainerHigh = Color(0xFF332D27),
    surfaceContainerHighest = Color(0xFF3E3731),
    outline = Color(0xFF8A7C6F),
    outlineVariant = Color(0xFF3D322B),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF4A1C1C),
    errorContainer = Color(0xFF5C2020),
    onErrorContainer = Color(0xFFFCD9D9),
)

@Composable
fun CaffeineTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
