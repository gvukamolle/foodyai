package com.example.calorietracker.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.background // <-- ДОБАВЬТЕ ЭТУ СТРОКУ

// Константы для анимаций
object AnimationConstants {
    const val FADE_IN_DURATION = 300
    const val FADE_OUT_DURATION = 200
    const val SCALE_DURATION = 250
    const val STAGGER_DELAY = 50L
    const val TYPE_WRITER_DELAY = 30L

    // Пружинные анимации
    val SPRING_SPEC = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val GENTLE_SPRING = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}

// Компонент для анимированного появления текста (эффект печатной машинки)
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    startDelay: Long = 0L,
    onComplete: () -> Unit = {}
) {
    var displayedText by remember(text) { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        if (startDelay > 0) delay(startDelay)
        text.forEachIndexed { index, _ ->
            delay(AnimationConstants.TYPE_WRITER_DELAY)
            displayedText = text.substring(0, index + 1)
        }
        onComplete()
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style
    )
}

// Анимированный индикатор "AI думает"
@Composable
fun AIThinkingIndicator(
    modifier: Modifier = Modifier,
    text: String = "AI анализирует"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // Анимация для точек
    val dotCount by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    // Пульсация масштаба
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Изменение прозрачности
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text + ".".repeat(dotCount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}

// Компонент для последовательного появления элементов списка
@Composable
fun <T> StaggeredAnimatedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    delayBetweenItems: Long = AnimationConstants.STAGGER_DELAY,
    content: @Composable (item: T, index: Int) -> Unit
) {
    items.forEachIndexed { index, item ->
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(items) {
            delay(index * delayBetweenItems)
            visible = true
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = tween(AnimationConstants.FADE_IN_DURATION)
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = tween(AnimationConstants.FADE_IN_DURATION)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = tween(AnimationConstants.FADE_OUT_DURATION)
            )
        ) {
            content(item, index)
        }
    }
}

// Анимированный прогресс для загрузки
@Composable
fun AnimatedLoadingDots(
    modifier: Modifier = Modifier,
    dotSize: Int = 12,
    color: Color = Color.Black
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(size = dotSize, color = color.copy(alpha = dot1Alpha))
        Dot(size = dotSize, color = color.copy(alpha = dot2Alpha))
        Dot(size = dotSize, color = color.copy(alpha = dot3Alpha))
    }
}

@Composable
private fun Dot(size: Int, color: Color) {
    Box(modifier = Modifier
            .size(size.dp)
            .graphicsLayer {
                clip = true
                shape = androidx.compose.foundation.shape.CircleShape
            }
        .background(color)) // <-- Ваша исправленная версия
}
