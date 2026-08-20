package com.kyssta.hermey.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kyssta.hermey.ui.HermesColors

private val DarkColorScheme = darkColorScheme(
    primary = HermesColors.Primary,
    onPrimary = HermesColors.OnPrimary,
    primaryContainer = HermesColors.PrimaryContainer,
    onPrimaryContainer = HermesColors.OnPrimaryContainer,
    secondary = HermesColors.Secondary,
    secondaryContainer = HermesColors.SecondaryContainer,
    tertiary = HermesColors.Tertiary,
    background = HermesColors.Background,
    onBackground = HermesColors.OnBackground,
    surface = HermesColors.Surface,
    onSurface = HermesColors.OnSurface,
    surfaceVariant = HermesColors.SurfaceVariant,
    onSurfaceVariant = HermesColors.OnSurfaceVariant,
    outline = HermesColors.Outline,
    error = HermesColors.Error,
)

private val LightColorScheme = lightColorScheme(
    primary = HermesColors.Primary,
    onPrimary = HermesColors.OnPrimary,
    primaryContainer = HermesColors.PrimaryContainer,
    onPrimaryContainer = HermesColors.OnPrimaryContainer,
    secondary = HermesColors.Secondary,
    secondaryContainer = HermesColors.SecondaryContainer,
    tertiary = HermesColors.Tertiary,
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFEEEEEE),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF48484A),
    outline = Color(0xFF747474),
    error = HermesColors.Error,
)

@Composable
fun HermexTheme(
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
        typography = Typography(),
        content = content
    )
}
