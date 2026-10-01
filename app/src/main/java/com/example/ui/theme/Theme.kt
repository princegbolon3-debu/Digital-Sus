package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = SusuEmeraldLight,
    onPrimary = SusuEmeraldDark,
    primaryContainer = SusuEmeraldPrimary,
    onPrimaryContainer = SusuEmeraldContainer,
    secondary = SusuGold,
    onSecondary = Color.Black,
    secondaryContainer = SusuGoldDark,
    onSecondaryContainer = SusuGoldContainer,
    background = SusuSurfaceDark,
    onBackground = SusuTextPrimaryDark,
    surface = SusuCardDark,
    onSurface = SusuTextPrimaryDark,
    surfaceVariant = Color(0xFF24332D),
    onSurfaceVariant = SusuTextSecondaryDark,
    outline = Color(0xFF3B4E46)
)

private val LightColorScheme = lightColorScheme(
    primary = SusuEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = SusuEmeraldContainer,
    onPrimaryContainer = SusuOnEmeraldContainer,
    secondary = SusuGoldDark,
    onSecondary = Color.White,
    secondaryContainer = SusuGoldContainer,
    onSecondaryContainer = SusuOnGoldContainer,
    background = SusuSurfaceLight,
    onBackground = SusuTextPrimaryLight,
    surface = SusuCardLight,
    onSurface = SusuTextPrimaryLight,
    surfaceVariant = Color(0xFFE5EEE9),
    onSurfaceVariant = SusuTextSecondaryLight,
    outline = Color(0xFFBACBC3)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve brand identity with Susu colors
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
