package com.laileme.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun LailemeTheme(
    content: @Composable () -> Unit
) {
    // 读取动态主色，Compose会自动在ThemeManager.currentPrimary变化时重组
    val dynamicPrimary = ThemeManager.currentPrimary
    val dynamicLightPrimary = ThemeManager.currentLightPrimary

    val colorScheme = lightColorScheme(
        primary = dynamicPrimary,
        onPrimary = Color.White,
        primaryContainer = dynamicLightPrimary,
        secondary = AccentTeal,
        tertiary = AccentOrange,
        background = Background,
        surface = CardBackground,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LailemeTypography,
        content = content
    )
}
