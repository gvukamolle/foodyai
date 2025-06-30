// ui/theme/Theme.kt

package com.example.calorietracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ИЗМЕНЕНИЯ ЗДЕСЬ
private val LightColorScheme = lightColorScheme(
    primary = AppBlack,
    onPrimary = Color.White,
    primaryContainer = AppGreyContainer,
    onPrimaryContainer = AppBlack,
    inversePrimary = AppBlack,
    secondary = AppDarkGrey,
    onSecondary = Color.White,
    secondaryContainer = AppGreyContainer,
    onSecondaryContainer = AppBlack,
    tertiary = AppDarkGrey,
    onTertiary = Color.White,
    tertiaryContainer = AppGreyContainer,
    onTertiaryContainer = AppBlack,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = AppGreyContainer,
    onBackground = AppBlack,
    onSurface = AppBlack,
    onSurfaceVariant = AppDarkGrey,
    outline = AppDarkGrey,
    outlineVariant = AppLightGrey,
    inverseSurface = AppBlack,
    inverseOnSurface = Color.White,
    surfaceTint = AppBlack,
)

@Composable
fun CalorieTrackerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}