package com.example.calorietracker.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color



/**
 * Централизованная конфигурация для всех анимаций в приложении
 */
object AnimationConfig {

    // Длительности анимаций
    object Duration {
        const val INSTANT = 50
        const val VERY_FAST = 100
        const val FAST = 150
        const val NORMAL = 200
        const val SLOW = 300
        const val VERY_SLOW = 500
    }

    // Константы жесткости пружин
    object Stiffness {
        const val VeryLow = Spring.StiffnessVeryLow
        const val Low = Spring.StiffnessMediumLow
        const val Medium = Spring.StiffnessMedium
        const val High = Spring.StiffnessHigh
        const val VeryHigh = 1000f // Очень быстрая пружина
    }

    // Спецификации анимаций для диалогов
    object Dialog {
        val fadeInSpec = tween<Float>(
            durationMillis = Duration.FAST,
            easing = FastOutSlowInEasing
        )

        val fadeOutSpec = tween<Float>(
            durationMillis = Duration.VERY_FAST,
            easing = FastOutLinearInEasing
        )

        val scaleInSpec = spring<Float>(
            dampingRatio = 0.8f,
            stiffness = 380f
        )

        val scaleOutSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )

        // Цвета для системных баров
        val systemBarsColor = Color.Black.copy(alpha = 0.6f)
        val backgroundStartColor = Color.Black.copy(alpha = 0.2f)
        val backgroundEndColor = Color.Black.copy(alpha = 0.6f)
    }

    // Анимации для кнопок
    object Button {
        val pressScale = 0.92f
        val hoverScale = 1.02f

        val pressSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    // Анимации для прогресс-баров
    object Progress {
        val colorTransition = tween<Color>(
            durationMillis = Duration.VERY_SLOW,
            easing = LinearEasing
        )

        val valueTransition = tween<Float>(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        )
    }
}
