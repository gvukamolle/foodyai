package com.example.calorietracker.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.extensions.fancyShadow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Компонент загрузки с анимированными звездочками
 * Звездочки "прыгают" по очереди, создавая эффект сверкания
 */
@Composable
fun SparklingStarsLoader(
    modifier: Modifier = Modifier,
    text: String = "AI анализирует описание..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    // Анимация для каждой из 5 звездочек
    val star1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star1"
    )

    val star2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 60, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star2"
    )

    val star3Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star3"
    )

    val star4Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star4"
    )

    val star5Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star5"
    )

    val scales = listOf(star1Scale, star2Scale, star3Scale, star4Scale, star5Scale)

    Box(
        modifier = modifier
            .width(200.dp) // Ограничиваем ширину
            .fancyShadow(
                color = Color.Black,
                alpha = 0.15f,
                borderRadius = 16.dp,
                shadowRadius = 12.dp,
                offsetY = 4.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Канва со звездочками
            Canvas(
                modifier = Modifier.size(80.dp)
            ) {
                // Рисуем 5 звездочек по кругу
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = 30.dp.toPx()

                for (i in 0..4) {
                    val angle = (i * 72 - 90) * (Math.PI / 180) // Начинаем сверху
                    val x = centerX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()

                    scale(scales[i], pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                        drawStar(
                            centerX = x,
                            centerY = y,
                            outerRadius = 8.dp.toPx(),
                            innerRadius = 4.dp.toPx(),
                            color = if (scales[i] > 1.0f) Color.Black else Color.Gray
                        )
                    }
                }
            }

            // Текст
            Text(
                text = text,
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Функция для рисования звездочки
private fun DrawScope.drawStar(
    centerX: Float,
    centerY: Float,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path().apply {
        val angleStep = Math.PI / 5

        for (i in 0..9) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * angleStep - Math.PI / 2
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()

            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }

    drawPath(path, color)
}