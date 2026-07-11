package com.youtubebridge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = YtRed,
    onPrimary = Color_White,
    secondary = GreenOnline,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkAlt,
    onBackground = OnDark,
    onSurface = OnDark,
    error = RedOffline
)

private val LightColors = lightColorScheme(
    primary = YtRed,
    onPrimary = Color_White,
    secondary = GreenOnline,
    background = BgLight,
    surface = SurfaceLight,
    onBackground = OnLight,
    onSurface = OnLight,
    error = RedOffline
)

private val Color_White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

@Composable
fun YouTubeBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
