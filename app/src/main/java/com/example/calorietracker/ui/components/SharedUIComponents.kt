package com.example.calorietracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.calorietracker.extensions.fancyShadow
import android.graphics.Color as AndroidColor

/**
 * Анимированная радужная обводка для выделения важных элементов UI.
 * Создает плавно переливающуюся радужную тень вокруг контента.
 *
 * @param modifier Модификатор для настройки компонента
 * @param borderWidth Ширина радужной обводки
 * @param cornerRadius Радиус скругления углов
 * @param content Содержимое внутри обводки
 */
@Composable
fun AnimatedRainbowBorder(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 12.dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")

    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )

    // Конвертируем HSV в RGB используя Android Color
    val hsv = floatArrayOf(hue, 1f, 1f)
    val rgb = AndroidColor.HSVToColor(hsv)
    val color = Color(rgb)

    Box(
        modifier = modifier
            .fancyShadow(
                color = color,
                borderRadius = cornerRadius,
                shadowRadius = borderWidth,
                alpha = 0.9f
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
    ) {
        Box(modifier = Modifier.padding(borderWidth)) {
            content()
        }
    }
}