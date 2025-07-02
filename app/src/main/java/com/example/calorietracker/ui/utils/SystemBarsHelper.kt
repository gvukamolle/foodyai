package com.example.calorietracker.ui.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Композиционная функция для временного изменения цвета системных баров
 * с автоматическим восстановлением при выходе из композиции
 */
@Composable
fun TemporarySystemBarsColor(
    statusBarColor: Color = Color.Black.copy(alpha = 0.4f),
    navigationBarColor: Color = Color.Black.copy(alpha = 0.4f),
    darkIcons: Boolean = false
) {
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current
    val context = LocalContext.current

    // Сохраняем оригинальные значения
    val originalStatusBarColor = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (context as? Activity)?.window?.statusBarColor ?: Color.Transparent.toArgb()
        } else Color.Transparent.toArgb()
    }

    val originalNavigationBarColor = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (context as? Activity)?.window?.navigationBarColor ?: Color.Transparent.toArgb()
        } else Color.Transparent.toArgb()
    }

    val originalDarkIcons = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = (context as? Activity)?.window?.decorView
            val flags = decorView?.systemUiVisibility ?: 0
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0
        } else false
    }

    DisposableEffect(statusBarColor, navigationBarColor, darkIcons) {
        // Применяем новые цвета
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = darkIcons
        )
        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = darkIcons
        )

        // Для Android 11+ также управляем edge-to-edge режимом
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = (context as? Activity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.insetsController?.setSystemBarsAppearance(
                    if (darkIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }

        onDispose {
            // Восстанавливаем оригинальные цвета
            systemUiController.setStatusBarColor(
                color = Color(originalStatusBarColor),
                darkIcons = originalDarkIcons
            )
            systemUiController.setNavigationBarColor(
                color = Color(originalNavigationBarColor),
                darkIcons = originalDarkIcons
            )
        }
    }
}

/**
 * Расширение для плавной анимации изменения цвета системных баров
 */
fun SystemUiController.animateSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkIcons: Boolean,
    animationDuration: Long = 200
) {
    // Здесь можно добавить анимацию через ValueAnimator если нужно
    setStatusBarColor(color = statusBarColor, darkIcons = darkIcons)
    setNavigationBarColor(color = navigationBarColor, darkIcons = darkIcons)
}
