package com.openpaperlabs.bolimoli.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BoliPurple,
    onPrimary = BoliSurfaceLight,
    primaryContainer = BoliPurpleContainer,
    onPrimaryContainer = BoliPurpleDark,
    secondary = BoliConsonantText,
    secondaryContainer = BoliConsonantContainer,
    background = BoliBackgroundLight,
    onBackground = BoliTextPrimaryLight,
    surface = BoliSurfaceLight,
    onSurface = BoliTextPrimaryLight,
    surfaceVariant = BoliBackgroundLight,
    onSurfaceVariant = BoliTextSecondaryLight,
    error = BoliError,
    errorContainer = BoliErrorContainer,
    onError = BoliSurfaceLight,
    onErrorContainer = BoliError
)

private val DarkColorScheme = darkColorScheme(
    primary = BoliPurpleContainer,
    onPrimary = BoliPurpleDark,
    primaryContainer = BoliPurpleContainerDark,
    onPrimaryContainer = BoliPurpleContainer,
    secondary = BoliConsonantContainer,
    secondaryContainer = BoliConsonantText,
    background = BoliBackgroundDark,
    onBackground = BoliTextPrimaryDark,
    surface = BoliSurfaceDark,
    onSurface = BoliTextPrimaryDark,
    surfaceVariant = BoliSurfaceDark,
    onSurfaceVariant = BoliTextSecondaryDark,
    error = BoliError,
    errorContainer = BoliErrorContainer,
    onError = BoliSurfaceLight,
    onErrorContainer = BoliErrorContainer
)

@Composable
fun BoliMoliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default false to ensure consistent BoliMoli child brand colors across all devices
    dynamicColor: Boolean = false,
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
        typography = Typography,
        content = content
    )
}

@Deprecated("Renamed to BoliMoliTheme", ReplaceWith("BoliMoliTheme(darkTheme, dynamicColor, content)"))
@Composable
fun PhonicstaINTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BoliMoliTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
