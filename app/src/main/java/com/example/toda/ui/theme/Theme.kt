package com.example.toda.ui.theme
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Dark theme
private val DarkColorScheme = darkColorScheme(
    primary = TealBlue80,
    secondary = TealBlue60,
    tertiary = TealBlue40,
    surface = Color.Black,        // 👈 dark cards in dark mode
    background = Color.Black      // 👈 dark background
)

// Light theme
private val LightColorScheme = lightColorScheme(
    primary = TealBlue80,
    secondary = TealBlue40,
    tertiary = TealBlue95,
    surface = Color.White,        // 👈 all cards, dialogs, surfaces will be white
    background = Color.White      // 👈 whole screen background white
)


@Composable
fun TODATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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