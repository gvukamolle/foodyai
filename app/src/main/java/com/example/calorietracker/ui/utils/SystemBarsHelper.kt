package com.example.calorietracker.ui.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Основной композабл для управления прозрачными системными барами
 * Используйте его как обертку для каждого экрана
 */
@Composable
fun TransparentSystemBars(
    darkIcons: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current
    
    DisposableEffect(darkIcons) {
        // Устанавливаем полностью прозрачные бары
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = darkIcons
        )
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = darkIcons
        )
        
        // Для Android 11+ также обновляем контроллер
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = (context as? Activity)?.window
            window?.let {
                val insetsController = it.insetsController
                if (darkIcons) {
                    insetsController?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or 
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    insetsController?.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            }
        }
        
        onDispose {
            // Оставляем прозрачность при выходе
        }
    }
    
    content()
}

/**
 * Модификатор для добавления отступов от системных баров
 */
@Composable
fun Modifier.systemBarsPadding(
    top: Boolean = true,
    bottom: Boolean = true,
    start: Boolean = false,
    end: Boolean = false
): Modifier {
    return this.then(
        if (top) Modifier.statusBarsPadding() else Modifier
    ).then(
        if (bottom) Modifier.navigationBarsPadding() else Modifier
    ).then(
        if (start || end) {
            Modifier.windowInsetsPadding(
                WindowInsets.systemBars.only(
                    if (start && end) WindowInsetsSides.Horizontal
                    else if (start) WindowInsetsSides.Start
                    else WindowInsetsSides.End
                )
            )
        } else Modifier
    )
}

/**
 * Получить высоту статус бара
 */
@Composable
fun getStatusBarHeight(): Dp {
    val density = LocalDensity.current
    val view = LocalView.current
    
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = view.rootWindowInsets?.getInsets(WindowInsetsCompat.Type.statusBars())
            with(density) { (insets?.top ?: 0).toDp() }
        } else {
            var result = 0
            val resourceId = view.context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = view.context.resources.getDimensionPixelSize(resourceId)
            }
            with(density) { result.toDp() }
        }
    }
}

/**
 * Получить высоту навигационного бара
 */
@Composable
fun getNavigationBarHeight(): Dp {
    val density = LocalDensity.current
    val view = LocalView.current
    
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = view.rootWindowInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())
            with(density) { (insets?.bottom ?: 0).toDp() }
        } else {
            var result = 0
            val resourceId = view.context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = view.context.resources.getDimensionPixelSize(resourceId)
            }
            with(density) { result.toDp() }
        }
    }
}

/**
 * Композиционная функция для временного изменения цвета системных баров
 * с автоматическим восстановлением при выходе из композиции
 * 
 * @deprecated Используйте TransparentSystemBars для прозрачности
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
