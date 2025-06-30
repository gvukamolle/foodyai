// ui/theme/Theme.kt

package com.example.calorietracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ИЗМЕНЕНИЯ ЗДЕСЬ
private val LightColorScheme = lightColorScheme(
    primary = AppBlack,       // ИСПРАВЛЕНО: был Purple40
    onPrimary = Color.White,  // Текст на черной кнопке будет белым

    secondary = AppDarkGrey,    // ИСПРАВЛЕНО: был PurpleGrey40
    onSecondary = Color.White,

    tertiary = AppDarkGrey,     // ИСПРАВЛЕНО: был Pink40 (можно тот же, что и secondary)
    onTertiary = Color.White,

    /* Остальные цвета можно оставить по умолчанию или настроить */
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = AppDarkGrey, // для второстепенного текста
    outline = AppLightGrey // для границ
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