package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KairoRed,
    onPrimary = Color.White,
    primaryContainer = KairoRedDark,
    onPrimaryContainer = Color.White,
    secondary = KairoRed,
    onSecondary = Color.White,
    tertiary = KairoDarkTextSecondary,
    background = KairoDarkBg,
    onBackground = KairoDarkTextPrimary,
    surface = KairoDarkSurface,
    onSurface = KairoDarkTextPrimary,
    surfaceVariant = KairoDarkSurfaceVariant,
    onSurfaceVariant = KairoDarkTextSecondary,
    outline = KairoDarkBorder,
    outlineVariant = Color(0x14FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = KairoRed,
    onPrimary = Color.White,
    primaryContainer = KairoRedDark,
    onPrimaryContainer = Color.White,
    secondary = KairoRed,
    onSecondary = Color.White,
    tertiary = KairoLightTextSecondary,
    background = KairoLightBg,
    onBackground = KairoLightTextPrimary,
    surface = KairoLightSurface,
    onSurface = KairoLightTextPrimary,
    surfaceVariant = KairoLightSurfaceVariant,
    onSurfaceVariant = KairoLightTextSecondary,
    outline = KairoLightBorder,
    outlineVariant = Color(0x0F000000)
)

@Composable
fun KairoMusicTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
