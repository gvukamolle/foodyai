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
    playAnimation: Boolean,
    startDelay: Long = 0L,
    modifier: Modifier = Modifier,
    onAnimationStart: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // 1. Запоминаем, нужно ли анимировать именно ЭТОТ экземпляр компонента.
    //    Это значение захватится при первой композиции и больше не изменится.
    val isAnimationTriggered by remember { mutableStateOf(playAnimation) }

    // 2. Состояние видимости. Изначально невидимо, если анимация запускается.
    var show by remember { mutableStateOf(!isAnimationTriggered) }

    // 3. LaunchedEffect запускается ОДИН РАЗ при входе компонента в композицию.
    LaunchedEffect(Unit) {
        // Если была команда на анимацию, выполняем ее.
        if (isAnimationTriggered) {
            delay(100) // Небольшая задержка для плавности
            if (startDelay > 0) delay(startDelay)
            show = true // Запускаем саму анимацию (меняем целевое значение)
            onAnimationStart() // Сообщаем ViewModel, что анимация для этого сообщения проиграна
        }
    }

    // 4. Спецификация анимации зависит от того, была ли она запущена изначально.
    val animationSpec = if (isAnimationTriggered) {
        tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
    } else {
        snap()
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = animationSpec,
        label = "alpha"
    )

    val animatedBlur by animateDpAsState(
        targetValue = if (show) 0.dp else 8.dp,
        animationSpec = if (isAnimationTriggered) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "blur"
    )

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