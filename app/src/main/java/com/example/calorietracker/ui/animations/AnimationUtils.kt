package com.example.calorietracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box

/* -------------------------------------------------------------------------- */
/*                              Сообщение в чате                              */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessage(
    visible: Boolean,
    isUserMessage: Boolean,
    startDelay: Long = 0L,
    modifier: Modifier = Modifier,
    onDisplayed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // Запоминаем, была ли уже проиграна анимация для этого сообщения
    var hasAnimated by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible && !hasAnimated) {
            // Задержка перед началом анимации (100 мс = 0.1 сек)
            delay(100)
            if (startDelay > 0) delay(startDelay)
            show = true
            hasAnimated = true
            onDisplayed()
        } else if (visible && hasAnimated) {
            // Если уже анимировали, просто показываем без анимации
            show = true
        } else {
            show = false
        }
    }

    // Анимация прозрачности - только если не анимировали ранее
    val animatedAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = if (!hasAnimated) {
            tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        } else {
            // Мгновенное появление для уже анимированных сообщений
            snap()
        },
        label = "alpha"
    )

    // Анимация размытия - только если не анимировали ранее
    val animatedBlur by animateDpAsState(
        targetValue = if (show && !hasAnimated) 0.dp else if (!show) 10.dp else 0.dp,
        animationSpec = if (!hasAnimated) {
            tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        } else {
            snap()
        },
        label = "blur"
    )

    if (show || animatedAlpha > 0f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = animatedAlpha
                }
                .blur(radius = animatedBlur)
        ) {
            content()
        }
    }
}

@Composable
fun Modifier.pulsate(): Modifier {
    val infinite = rememberInfiniteTransition(label = "pulsate-transition")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsate-scale"
    )
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}