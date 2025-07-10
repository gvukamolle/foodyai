package com.example.calorietracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

/* -------------------------------------------------------------------------- */
/*                              Сообщение в чате                              */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessage(
    id: Any,
    visible: Boolean,
    isUserMessage: Boolean,
    startDelay: Long = 0L,
    modifier: Modifier = Modifier,
    onDisplayed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // Запоминаем состояние анимации отдельно для каждого сообщения
    var hasAnimated by remember(id) { mutableStateOf(false) }
    var show by remember(id) { mutableStateOf(false) }

    LaunchedEffect(visible, id) {
        if (visible && !hasAnimated) {
            // Задержка перед началом анимации (100 мс = 0.1 сек)
            delay(100)
            if (startDelay > 0) delay(startDelay)
            show = true
            hasAnimated = true
            onDisplayed()
        } else if (visible && hasAnimated) {
            // Если уже анимировали, просто показываем без задержки
            show = true
        } else if (!visible) {
            show = false
        }
    }

    // Анимация прозрачности - только для новых сообщений
    val animatedAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = if (!hasAnimated || !show) {
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

    // Анимация размытия - только для новых сообщений
    val animatedBlur by animateDpAsState(
        targetValue = if (show) 0.dp else 10.dp,
        animationSpec = if (!hasAnimated || !show) {
            tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        } else {
            snap()
        },
        label = "blur"
    )

    if (visible) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = animatedAlpha
                }
                .then(
                    if (!hasAnimated || animatedBlur > 0.dp) {
                        Modifier.blur(radius = animatedBlur)
                    } else {
                        Modifier
                    }
                )
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