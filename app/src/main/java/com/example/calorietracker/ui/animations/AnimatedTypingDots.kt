package com.example.calorietracker.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Анимированные точки с переливающимся градиентом
 * Имитируют процесс набора текста ИИ
 */
@Composable
fun AnimatedTypingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    dotSpacing: Dp = 6.dp,
    primaryColor: Color = Color(0xFF2196F3),
    secondaryColor: Color = Color(0xFF9C27B0),
    animationDuration: Int = 800
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    // Анимация для переливающегося градиента
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    // Анимации для каждой точки
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, delayMillis = animationDuration / 3),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, delayMillis = (animationDuration * 2) / 3),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    // Анимация масштаба для эффекта пульсации
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Scale"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, delayMillis = animationDuration / 3),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Scale"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, delayMillis = (animationDuration * 2) / 3),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Scale"
    )

    // Создаем переливающийся градиент
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            primaryColor,
            secondaryColor,
            primaryColor
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1000f * gradientOffset, 0f)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Точка 1
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    alpha = dot1Alpha
                    scaleX = dot1Scale
                    scaleY = dot1Scale
                }
                .clip(CircleShape)
                .background(gradientBrush)
        )

        // Точка 2
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    alpha = dot2Alpha
                    scaleX = dot2Scale
                    scaleY = dot2Scale
                }
                .clip(CircleShape)
                .background(gradientBrush)
        )

        // Точка 3
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    alpha = dot3Alpha
                    scaleX = dot3Scale
                    scaleY = dot3Scale
                }
                .clip(CircleShape)
                .background(gradientBrush)
        )
    }
}

/**
 * Компонент сообщения с анимированными точками
 * Используется вместо статичного текста "Обрабатываю ваш запрос..."
 */
@Composable
fun AIProcessingMessage(
    modifier: Modifier = Modifier,
    text: String = "AI обрабатывает",
    showDots: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            color = Color.Black.copy(alpha = 0.7f)
        )

        if (showDots) {
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedTypingDots(
                dotSize = 8.dp,
                dotSpacing = 4.dp,
                primaryColor = Color(0xFF4CAF50),
                secondaryColor = Color(0xFF2196F3)
            )
        }
    }
}

/**
 * Минималистичная версия для встраивания в чат
 */
@Composable
fun SimpleChatTypingIndicator(
    modifier: Modifier = Modifier
) {
    AnimatedTypingDots(
        modifier = modifier,
        dotSize = 6.dp,
        dotSpacing = 3.dp,
        primaryColor = Color.Black.copy(alpha = 0.6f),
        secondaryColor = Color.Black.copy(alpha = 0.3f),
        animationDuration = 600
    )
}